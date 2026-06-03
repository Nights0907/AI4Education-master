package org.musi.AI4Education.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import javax.annotation.Resource;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.SystemMessage;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.bson.Document;
import org.json.JSONException;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.config.Wen_XinConfig;
import org.musi.AI4Education.domain.*;
import org.musi.AI4Education.mapper.ConcreteQuestionMapper;
import org.musi.AI4Education.model.wenxin.WenxinChatModel;
import org.musi.AI4Education.service.AiCodeHelperService;
import org.musi.AI4Education.service.BasicQuestionService;
import org.musi.AI4Education.service.ConcreteQuestionService;
import org.musi.AI4Education.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ConcreteQuestionServiceImpl extends ServiceImpl<ConcreteQuestionMapper, ConcreteQuestion> implements ConcreteQuestionService {

    private Map<String, ChatSession> sessions = new HashMap<>(); // Store sessions using user IDs
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private BasicQuestionService basicQuestionService;
    @Autowired
    private StudentService studentService;

    @Resource
    private Wen_XinConfig wenXinConfig;
    //历史对话，需要按照user,assistant
    List<Map<String,String>> messages = new ArrayList<>();

    private final ConcurrentMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    List<Map<String,String>> listMessage = new ArrayList<>();

    List<Map<String,String>> listMessage1 = new ArrayList<>();

    @Resource
    private AiCodeHelperService aiCodeHelperService;

    @Resource
    private ContentRetriever contentRetriever;

    @Resource
    private WenxinChatModel wenxinChatModel;

    /**
     * 构造请求的请求参数
     * @param userId
     * @param temperature
     * @param topP
     * @param penaltyScore
     * @param messages
     * @return
     */
    @Override
    public String constructRequestJson(Integer userId,
                                       Double temperature,
                                       Double topP,
                                       Double penaltyScore,
                                       boolean stream,
                                       List<Map<String, String>> messages) {
        Map<String,Object> request = new HashMap<>();
        request.put("model", wenXinConfig.MODEL);
        request.put("temperature",temperature);
        request.put("top_p",topP);
        request.put("stream",stream);
        request.put("messages",messages);
        System.out.println(JSON.toJSONString(request));
        return JSON.toJSONString(request);
    }

    @Override
    public String useWenxinStreamTransformToGetAnswerAndExplanationAndKnowledge(String question) throws IOException {
        String ststemMessage = "我将会传输带有latex公式的数学题目，只需要给出(1)题目的标准答案(2)题目的简略解析(3)题目考察的与数学相关的知识点，将答案、简略解析以及知识点分别用[]括起来，例如“[答案：2],[解析：1+1=2],[{加法原理},{乘法原理}]”,记住不能使用诸如计算器、近似值的办法，下面是题目：";
        return connectWithBigModelStreamTransition(ststemMessage + question);
    }

    @Override
    public Map<String, String> useWenxinStreamTransformToAnalyseAbilityByKnowledge(List<String> knowledge) throws IOException {

        String question =
                "<content>"+
                "我将会以 List<String> 的方式传输知识点列表，请帮我分析每个知识点所考察的核心素养。"+
                "核心素养能力包括以下六个方面：数学抽象、数学建模、数据分析、直观想象、逻辑推理、数学运算。下面是六个能力的具体解释："+
                "</content>"+
                "<explanation>"+
                "数学抽象：这是指将具体问题或概念抽象成数学符号、结构或模型的能力。通过数学抽象，我们能够将复杂的现实世界问题简化成数学问题，从而更容易进行分析和解决。"+
                "数学建模：数学建模是将实际问题转化为数学模型的过程。这涉及到选择适当的数学工具和技术，以及对问题进行合理的简化和假设，以便于用数学语言描述和分析。"+
                "数据分析：数据分析是指利用数学、统计学和计算机科学等方法对数据进行处理、解释和推断的过程。这包括数据清洗、探索性数据分析、统计建模、机器学习等技术。"+
                "直观想象：这是指通过直觉和想象力理解和解决问题的能力。在数学中，直观想象可以帮助我们形成几何图像、推断模式或者预测结果，从而指导数学推理和解决问题的过程。"+
                "逻辑推理：逻辑推理是指根据已知信息和逻辑规则，推导出新的结论或解决问题的过程。在数学中，逻辑推理是证明定理、解决问题和构建数学理论的基本方法之一。"+
                "数学运算：数学运算是指进行数值计算、代数运算、微积分等基本数学操作的能力。这包括加减乘除、求导、积分等操作，是进行数学分析和解决实际问题的基础。"+
                "</explanation>"+
                "<goal>"+
                "请根据每一个知识点，分析其考察的核心素养能力，我会给你一个例子. \n"+
                "</goal>"+
                "<input>"+
                "{函数单调性运算,立体几何}"+
                "</input>"+
                "<output>"+
                "[函数单调性运算:{数学分析,逻辑推理}],"+
                "[立体几何:{直观想象,数学建模}]"+
                "</output>"+
                "下面是知识点列表"+knowledge+"请只返回如<output>那样的知识点与能力的映射关系,不用带上<output>";

        String result = connectWithBigModelStreamTransition(question);
        Map<String, String> resultMap = parseStringToObject(result);
        return resultMap;
    }


    @Override
    public String useWenxinStreamTransformToGetSteps(String question) throws IOException {
        return answerWithRag(question);
    }

    // RAG 部分
    public String answerWithRag(String query) {
        List<Content> contents = contentRetriever.retrieve(dev.langchain4j.rag.query.Query.from(query));
        String context = contents.stream().map(Object::toString).collect(Collectors.joining("\n\n"));

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(dev.langchain4j.data.message.SystemMessage.from("你是一个乐于助人的中文助理。请基于提供的检索上下文回答问题，若上下文无关则直说不知道。"));

        String formatted = "你是一名擅长中学数学讲解的助教。现在请严格按照以下规则，给出题目的解题步骤：\\n"
                + "1) 仅输出步骤列表，不要输出任何额外说明、前后缀或空行。\\n"
                + "2) 每一个独立步骤必须完整地放在一对中括号[]内，且每个[]内部以阿拉伯数字开头编号，如：[1. ...]、[2. ...]。\\n"
                + "3) 所有步骤都要覆盖从审题到列式、运算、化简、结论或单位等关键环节，保证逻辑清晰、面向初高中生、语言简洁。\\n"
                + "4) 题目中可能包含 LaTeX 公式，请原样保留并正确使用，不要破坏公式格式。\\n"
                + "5) 严禁使用计算器或近似值，严禁跳步或省略关键推导，若有条件或结论需要说明，请在对应步骤的[]内简要说明。\\n"
                + "6) 除了[]步骤外，禁止出现任何其他文本、标点或空白行（包括“解：”“答：”“总结”等）。\\n"
                + "示例格式（仅示意，非该题答案）：\\n"
                + "[1. 审题与已知：……]\\n"
                + "[2. 设未知量并转化：……]\\n"
                + "[3. 列方程/函数/几何关系（保留 LaTeX）：……]\\n"
                + "[4. 变形与求解（逐步推导、保留 LaTeX）：……]\\n"
                + "[5. 结论与检验：……]\\n"
                + "现在请仅输出满足以上规则的步骤列表。";;
        messages.add(dev.langchain4j.data.message.SystemMessage.from(formatted));

        messages.add(UserMessage.from("上下文：\n" + context + "\n\n问题：" + query));

        dev.langchain4j.model.output.Response<AiMessage> resp = wenxinChatModel.generate(messages);
        return resp == null || resp.content() == null ? "" : resp.content().text();
    }

    @Override
    public List<String> useWenxinStreamTransformToAnalyseWrongType(String question, String content) throws IOException, JSONException {
        String sid = StpUtil.getLoginIdAsString();
        String description = studentService.getStudentBySid(sid).getDescription();

        String require =
                "我需要你分析学生在解决数学问题时生成错误解题步骤的错误类型,该学生情况为："+description+"，请结合学生的个人情况，并结合学生的错误答案，以教师的口吻，重复一下学生情况，并设计一个教学方案" +
                        "你需要提在下列范围内，寻找一个（只有一个）最为接近的基本类型与一个（只有一个）细分类型\n" +

                        "基本类型列表为：" +
                        "{计算错误、概念错误、读题错误、解题错误}\n" +

                        "对应的细分类型为："+
                        "[{\"计算错误\":\"{代数,正负值错误,单位转换,值错误}\"},"+
                        " {\"概念错误\":\"{理解错误,抄写题目信息疏忽}\"},"+
                        " {\"读题错误\":\"{方程设立错误,错误格式,概念遗忘}\"},"+
                        " {\"解题错误\":\"{解题步骤不完整,结论错误,猜答案}\"}]"+

                        "如果没有与之匹配的的基本类型与细分类型，请新建一个基本类型与细分类型！\n"+

                        "下面是原始问题:"+question+
                        "下面是学生提供的错误解题步骤:"+content+
                        "请分析学生所犯的错误类型，并且只返回一个基本类型 与 一个细分类型,与一份教学方案，不要过多解释！\n" +
                        "基本类型：用[]括起来 "+
                        "细分类型：用[]括起来 "+
                        "教学方案：用[]括起来 ";


        String stringWithAnswer = connectWithBigModelStreamTransition(require);
        System.out.println(stringWithAnswer);

        List<String> resultList = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(stringWithAnswer);

        while (matcher.find()) {
            resultList.add(matcher.group(1));
        }
        return resultList;
    }

    @Override
    public Flux<String> useWenxinStreamTransformToCommunicateWithUser(String sid,String qid, String content) throws IOException, JSONException {

        String qidForChatHistory = qid+"001";
        BasicQuestion basicQuestion = new BasicQuestion();
        basicQuestion.setQid(qid);
        String question = String.valueOf(basicQuestionService.getQuestionTextByQid(basicQuestion));

        // 直接尝试获取会话对象
        ChatSession session = sessions.get(qidForChatHistory);

        if (session == null) { // 如果获取的会话对象为空
            // 说明这是第一次创建
            session = new ChatSession(); // 创建新的会话对象
            sessions.put(qidForChatHistory, session); // 将新的会话对象放入 sessions 中
            // 在这里可以执行第一次创建会话的相关逻辑
            String front = "我将提供一个含有LaTex公式的数学题目，请根据这个题目回答下列问题，题目为：";
            content =  front + question+" 请回答我的提问："+content;
        }


        //设置请求体 这一部分可以放到Service
        HashMap<String, String> user = new HashMap<>();
        user.put("role","user");
        user.put("content",content);
        listMessage.add(user);
        String requestJson = constructRequestJson(1,0.95,0.8,1.0,true,listMessage);

        //进行数据访问 返回String类型的数据
        StringBuffer answer=new StringBuffer();
        return deepSeekWebClient().post().uri(wenXinConfig.CHAT_COMPLETIONS_PATH)
                .bodyValue(requestJson)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(data -> Flux.fromIterable(extractStreamContents(data)))
                .map(result -> result.replace("\n", " "))
                .doOnNext(result -> {
                    System.out.println(result);
                    answer.append(result);
                }).doOnComplete(() -> {
                    // 当Flux完成时，输出结束消息
                    System.out.println("处理完毕，流已关闭。");
                    System.out.println(answer);
                    //将回复的内容添加到消息中
                    HashMap<String, String> assistant = new HashMap<>();
                    assistant.put("role","assistant");
                    assistant.put("content",answer.toString());
                    listMessage.add(assistant);

                    //将数据存入MongoDB数据库

                    // 组合多个查询条件，并在MongoDB中查询
                    Criteria criteria1 = Criteria.where("sid").is(sid);
                    Criteria criteria2 = Criteria.where("qid").is(qidForChatHistory);
                    Criteria criteria = new Criteria().andOperator(criteria1, criteria2);
                    Query query = new Query(criteria);
                    List<ChatHistory> result = mongoTemplate.find(query, ChatHistory.class);


                    List<HashMap<String,String>> chatHistoryTemp = new ArrayList<>();
                    //存入MongoDB
                    //获取当前该用户的该题的聊天记录
                    for (Map<String, String> message : listMessage) {
                        String role1 = message.get("role");
                        String content1 = message.get("content");
                        HashMap<String,String> temp = new HashMap<>();
                        temp.put(role1,content1);
                        chatHistoryTemp.add(temp);
                    }

                    if (result.isEmpty()) {
                        System.out.println("查询结果为空");
                        ChatHistory chatHistory = new ChatHistory();
                        chatHistory.setQid(qidForChatHistory);
                        chatHistory.setSid(sid);
                        chatHistory.setWenxinChatHistory(chatHistoryTemp);
                        mongoTemplate.insert(chatHistory);
                    } else {
                        System.out.println("查询结果不为空");
                        Update update = new Update().set("wenxinChatHistory",chatHistoryTemp);
                        mongoTemplate.updateFirst(query, update, "chatHistory");
                    }

                }).log();
    }

    @Override
    public Flux<String> useWenxinStreamTransformToCommunicateWithUserWithWrongAnswer(String sid,String qid, String wrongText, String wrongReason, String content) throws IOException, JSONException {

        String qidForChatHistory = qid+"002";
        BasicQuestion basicQuestion = new BasicQuestion();
        basicQuestion.setQid(qid);
        String question = String.valueOf(basicQuestionService.getQuestionTextByQid(basicQuestion));


        // 直接尝试获取会话对象
        ChatSession session = sessions.get(qidForChatHistory);

        if (session == null) { // 如果获取的会话对象为空
            // 说明这是第一次创建
            session = new ChatSession(); // 创建新的会话对象
            sessions.put(qidForChatHistory, session); // 将新的会话对象放入 sessions 中
            // 在这里可以执行第一次创建会话的相关逻辑
            String front = "我将提供一个含有LaTex公式的数学题目，并提供我的有错误的解题思路，以及该我犯错误的原因.\n";
            content =  front+"题目为："+question+"  我的错解为"+wrongText+"  我的错因为："+wrongReason+ " 请结合我犯错误的原因，以教师的口吻分析我犯错的地方在哪";
        }

        //设置请求体 这一部分可以放到Service
        HashMap<String, String> user = new HashMap<>();
        user.put("role","user");
        user.put("content",content);
        listMessage1.add(user);
        String requestJson = constructRequestJson(1,0.95,0.8,1.0,true,listMessage1);

        //进行数据访问 返回String类型的数据
        StringBuffer answer=new StringBuffer();
        return deepSeekWebClient().post().uri(wenXinConfig.CHAT_COMPLETIONS_PATH)
                .bodyValue(requestJson)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(data -> Flux.fromIterable(extractStreamContents(data)))
                .map(result -> result.replace("\n", " "))
                .doOnNext(result -> {
                    System.out.println(result);
                    answer.append(result);
                }).doOnComplete(() -> {
                    // 当Flux完成时，输出结束消息
                    System.out.println("处理完毕，流已关闭。");
                    System.out.println(answer);
                    //将回复的内容添加到消息中
                    HashMap<String, String> assistant = new HashMap<>();
                    assistant.put("role","assistant");
                    assistant.put("content",answer.toString());
                    listMessage1.add(assistant);

                    //将数据存入MongoDB数据库

                    // 组合多个查询条件，并在MongoDB中查询
                    Criteria criteria1 = Criteria.where("sid").is(sid);
                    Criteria criteria2 = Criteria.where("qid").is(qidForChatHistory);
                    Criteria criteria = new Criteria().andOperator(criteria1, criteria2);
                    Query query = new Query(criteria);
                    List<WrongReasonChatHistory> result = mongoTemplate.find(query, WrongReasonChatHistory.class);


                    List<HashMap<String,String>> chatHistoryTemp = new ArrayList<>();
                    //存入MongoDB
                    //获取当前该用户的该题的聊天记录
                    for (Map<String, String> message : listMessage1) {
                        String role1 = message.get("role");
                        String content1 = message.get("content");
                        HashMap<String,String> temp = new HashMap<>();
                        temp.put(role1,content1);
                        chatHistoryTemp.add(temp);
                    }

                    if (result.isEmpty()) {
                        System.out.println("查询结果为空");
                        WrongReasonChatHistory chatHistory = new WrongReasonChatHistory();
                        chatHistory.setQid(qidForChatHistory);
                        chatHistory.setSid(sid);
                        chatHistory.setWenxinChatHistory(chatHistoryTemp);
                        mongoTemplate.insert(chatHistory);
                    } else {
                        System.out.println("查询结果不为空");
                        Update update = new Update().set("wenxinChatHistory",chatHistoryTemp);
                        mongoTemplate.updateFirst(query, update, "wrongReasonChatHistory");
                    }

                }).log();
    }


    @Override
    public String getQuestionStepByQuestionNumber(String qid, int targetNumber) {
        // 构建查询条件，匹配指定 id 的文档
        Query query = new Query(Criteria.where("qid").is(qid));
        // 查询指定 qid 的文档
        Document document = mongoTemplate.findOne(query, Document.class, "concreteQuestion");
        // 如果找到了文档
        if (document != null) {
            // 获取 questionSteps 数组
            List<Document> questionSteps = (List<Document>) document.get("questionSteps");

            // 遍历 questionSteps 数组
            for (Document step : questionSteps) {
                // 获取当前步骤的数字
                // 如果当前步骤的数字与目标数字匹配
                int number = step.getInteger("number");
                if (number == targetNumber) {
                    // 获取当前步骤的 content 值
                    return step.getString("content");
                }
            }
        }
        // 如果未找到匹配的 content，则返回空字符串或者 null，视需求而定
        return null;

    }

    @Override
    public List<String> getQuestionKnowledgesByQid(String qid) {
        // 构建查询条件，匹配指定 id 的文档
        Query query = new Query(Criteria.where("qid").is(qid));
        // 查询指定 qid 的文档
        Document document = mongoTemplate.findOne(query, Document.class, "concreteQuestion");
        // 如果找到了文档
        if (document != null) {
            // 获取 questionSteps 数组
            List<String> questionKnowledges = (List<String>) document.get("knowledges");
            List<String> stringList = new ArrayList<>();
            // 遍历 questionSteps 数组
            for (String step : questionKnowledges) {
                stringList.add(String.valueOf(step));
                }
            return stringList;
            }
        // 如果未找到匹配的 content，则返回空字符串或者 null，视需求而定
        return null;
    }

    @Override
    public String uploadQuestionNotesByQid(String qid,String note) {
        Query query = new Query(Criteria.where("qid").is(qid));
        Update update = new Update();
        update.set("note", note);
        mongoTemplate.updateFirst(query, update, "concreteQuestion"); // 你的文档类名替换为实际的类名
        return note;
    }

    @Override
    public ConcreteQuestion getQuestionNotesByQid(String qid) {
        Query query = new Query(Criteria.where("qid").is(qid));
        ConcreteQuestion result = mongoTemplate.findOne(query, ConcreteQuestion.class); // 你的文档类名替换为实际的类名
        return result;

    }

    @Override
    public String modifyQuestionNotesByQid(String qid, String note) {
        Query query = new Query(Criteria.where("qid").is(qid));
        Update update = new Update();
        update.set("note", note);
        mongoTemplate.updateFirst(query, update, "concreteQuestion"); // 你的文档类名替换为实际的类名
        return note;
    }

    @Override
    public String deleteQuestionNotesByQid(String qid) {
        Query query = new Query(Criteria.where("qid").is(qid));
        Update update = new Update();
        update.set("note", "");
        mongoTemplate.updateFirst(query, update, "concreteQuestion"); // 你的文档类名替换为实际的类名
        return "删除成功";
    }



    @Override
    public ChatHistory getChatHistoryByQid(String qid) throws IOException {
        Criteria criteria1 = Criteria.where("sid").is(StpUtil.getLoginIdAsString());
        Criteria criteria2 = Criteria.where("qid").is(qid+"001");
        // 组合多个查询条件
        Criteria criteria = new Criteria().andOperator(criteria1, criteria2);
        // 创建查询对象
        Query query = new Query(criteria);
        ChatHistory result = mongoTemplate.findOne(query, ChatHistory.class);
        return result;
    }

    @Override
    public WrongReasonChatHistory getWrongAnswerChatHistoryByQid(String qid) throws IOException {
        Criteria criteria1 = Criteria.where("sid").is(StpUtil.getLoginIdAsString());
        Criteria criteria2 = Criteria.where("qid").is(qid+"002");
        // 组合多个查询条件
        Criteria criteria = new Criteria().andOperator(criteria1, criteria2);
        // 创建查询对象
        Query query = new Query(criteria);
        WrongReasonChatHistory result = mongoTemplate.findOne(query, WrongReasonChatHistory.class);
        return result;
    }

    @Override
    public CommonResponse<String> createConcreteQuestion(ConcreteQuestion concreteQuestion) {
        mongoTemplate.insert(concreteQuestion);
        return CommonResponse.creatForSuccess("添加成功");
    }

    @Override
    public ArrayList<QuestionStep> createQuestionSteps(String steps) {

        ArrayList<QuestionStep> questionStepList = new ArrayList<QuestionStep>();

        ArrayList<String> contents = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[([^\\[\\]]*)\\]"); // 匹配被"[]"括起来的内容
        Matcher matcher = pattern.matcher(steps);

        // 使用正则表达式逐个匹配并提取内容
        while (matcher.find()) {
            contents.add(matcher.group(1)); // group(1) 提取括号中的内容
        }
        int step = 1;
        for (String result : contents) {
            QuestionStep questionStep = new QuestionStep();
            questionStep.setNumber(step++);
            questionStep.setContent(result);
            questionStepList.add(questionStep);
        }

        return questionStepList;
    }

    @Override
    public List<String> splitAnswerAndExplanation(String steps) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[([^\\[\\]]*)\\]"); // 匹配被"[]"括起来的内容
        Matcher matcher = pattern.matcher(steps);

        // 使用正则表达式逐个匹配并提取内容
        while (matcher.find()) {
            result.add(matcher.group(1));
            System.out.println(matcher.group(1));
        }
        return result;
    }

    @Override
    public List<String> splitKnowledges(String knowledges) {
        List<String> result = new ArrayList<>();

        Pattern pattern = Pattern.compile("\\{(.*?)\\}"); // 匹配被"[]"括起来的内容
        Matcher matcher = pattern.matcher(knowledges);

        // 使用正则表达式逐个匹配并提取内容
        while (matcher.find()) {
            result.add(matcher.group(1)); // group(1) 提取括号中的内容
        }
        return result;
    }

    @Override
    public ConcreteQuestion getConcreteQuestionByQid(ConcreteQuestion concreteQuestion) {
        Query query = new Query();
        query.addCriteria(Criteria.where("qid").is(concreteQuestion.getQid()));
        ConcreteQuestion concreteQuestion1 = mongoTemplate.findOne(query, ConcreteQuestion.class);
        return concreteQuestion1;
    }

    @Override
    public JSON connectWithBigModel(String content) throws IOException {
        HashMap<String, String> msg = new HashMap<>();
        msg.put("role","user");
        msg.put("content", content);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(msg);
        String requestJson = constructRequestJson(1,0.95,0.8,1.0,false,messages);
        Request request = new Request.Builder()
                .url(wenXinConfig.chatCompletionsUrl())
                .method("POST", RequestBody.create(MediaType.parse("application/json"), requestJson))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", wenXinConfig.authorizationHeader())
                .build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            return JSON.parseObject(body);
        }
    }


    @Override
    @SystemMessage(fromResource = "system_prompt.txt")
    public String chatWithRAG(String question) throws IOException {

        HashMap<String, String> user = new HashMap<>();
        user.put("role","user");
        user.put("content", question);
        messages.add(user);

        // 使用collectList收集所有块，然后阻塞直到完成
        List<String> chunks = aiCodeHelperService.chatStream(question)
                .collectList()
                .block(); // 阻塞直到所有数据块收集完成

        String completeResponse = String.join("", chunks);
        // 处理完整响应

        System.out.println(completeResponse);

        return completeResponse;
    }


    public String connectWithBigModelStreamTransition(String question) throws IOException {
        HashMap<String, String> user = new HashMap<>();
        user.put("role","user");
        user.put("content",question);
        messages.add(user);
        String requestJson = constructRequestJson(1,0.95,0.8,1.0,true,messages);
        String answer = executeDeepSeekStream(requestJson, null);
        HashMap<String, String> assistant = new HashMap<>();
        assistant.put("role","assistant");
        assistant.put("content",answer);
        messages.add(assistant);
        return answer;
    }

    @Override
    public String test(String content) throws IOException {

        Long qid = Long.valueOf("123456"+"001");
        SseEmitter emitter = new SseEmitter();
        emitters.put(qid, emitter);

        System.out.println(content);

        HashMap<String, String> user = new HashMap<>();
        user.put("role","user");
        user.put("content",content);
        messages.add(user);
        String requestJson = constructRequestJson(1,0.95,0.8,1.0,true,messages);
        String answer = executeDeepSeekStream(requestJson, data -> sendDataToClient(qid, data));
        HashMap<String, String> assistant = new HashMap<>();
        assistant.put("role","assistant");
        assistant.put("content",answer);
        messages.add(assistant);

        emitter.onCompletion(() -> emitters.remove(emitter.hashCode()));
        emitter.onTimeout(() -> emitters.remove(emitter.hashCode()));

        return answer;
    }

    @Override
    public void sendDataToClient(Long clientId, String data) {

        SseEmitter emitter = emitters.get(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().data(data));
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(clientId);
            }
        }
    }
    private WebClient deepSeekWebClient() {
        return WebClient.builder()
                .baseUrl(wenXinConfig.BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, wenXinConfig.authorizationHeader())
                .defaultHeader(HttpHeaders.ACCEPT, org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
                .build();
    }

    private String executeDeepSeekStream(String requestJson, java.util.function.Consumer<String> onChunk) throws IOException {
        Request request = new Request.Builder()
                .url(wenXinConfig.chatCompletionsUrl())
                .method("POST", RequestBody.create(MediaType.parse("application/json"), requestJson))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .addHeader("Authorization", wenXinConfig.authorizationHeader())
                .build();
        StringBuilder answer = new StringBuilder();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("DeepSeek请求失败: {}", response.code());
                return "";
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return "";
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    for (String content : extractStreamContents(line)) {
                        answer.append(content);
                        if (onChunk != null) {
                            onChunk.accept(content);
                        }
                    }
                }
            }
        }
        return answer.toString();
    }

    private List<String> extractStreamContents(String data) {
        List<String> contents = new ArrayList<>();
        if (data == null || data.isBlank()) {
            return contents;
        }
        String[] lines = data.split("\\r?\\n");
        for (String line : lines) {
            String payload = line.trim();
            if (payload.isEmpty()) {
                continue;
            }
            if (payload.startsWith("data:")) {
                payload = payload.substring(5).trim();
            }
            if (payload.isEmpty() || "[DONE]".equals(payload)) {
                continue;
            }
            try {
                com.alibaba.fastjson.JSONObject root = com.alibaba.fastjson.JSONObject.parseObject(payload);
                com.alibaba.fastjson.JSONObject error = root.getJSONObject("error");
                if (error != null) {
                    log.error("DeepSeek返回错误: {}", error.getString("message"));
                    continue;
                }
                com.alibaba.fastjson.JSONArray choices = root.getJSONArray("choices");
                if (choices == null || choices.isEmpty()) {
                    continue;
                }
                com.alibaba.fastjson.JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                if (delta == null) {
                    continue;
                }
                String content = delta.getString("content");
                if (content != null && !content.isEmpty()) {
                    contents.add(content);
                }
            } catch (Exception e) {
                log.warn("忽略无法解析的DeepSeek流式片段");
            }
        }
        return contents;
    }

    @Override
    public Map<String, String> parseStringToObject(String data) {
        Map<String, String> resultMap = new HashMap<>();
        String[] pairs = data.split("\\],\\[");
        for (String pair : pairs) {
            pair = pair.replaceAll("[\\[\\]]", ""); // Remove square brackets
            String[] keyValue = pair.split(":\\{");
            String key = keyValue[0];
            String value = keyValue[1].replaceAll("\\}", "");
            resultMap.put(key, value);
        }
        return resultMap;
    }

}
