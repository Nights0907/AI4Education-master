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
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.bson.Document;
import org.json.JSONException;
import org.musi.AI4Education.common.CommonResponse;
import org.musi.AI4Education.config.Wen_XinConfig;
import org.musi.AI4Education.domain.*;
import org.musi.AI4Education.mapper.ConcreteQuestionMapper;
import org.musi.AI4Education.model.wenxin.WenxinChatModel;
import org.musi.AI4Education.prompt.PromptKeys;
import org.musi.AI4Education.service.AiCodeHelperService;
import org.musi.AI4Education.service.BasicQuestionService;
import org.musi.AI4Education.service.ConcreteQuestionService;
import org.musi.AI4Education.service.PromptTemplateService;
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
    @Resource
    private PromptTemplateService promptTemplateService;
    //历史对话，需要按照user,assistant
    List<Map<String,String>> messages = new ArrayList<>();

    private final ConcurrentMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

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
        List<Map<String, String>> requestMessages = new ArrayList<>();
        for (Map<String, String> message : messages) {
            Map<String, String> requestMessage = new HashMap<>();
            requestMessage.put("role", message.get("role"));
            requestMessage.put("content", message.get("content"));
            requestMessages.add(requestMessage);
        }
        request.put("messages",requestMessages);
        System.out.println(JSON.toJSONString(request));
        return JSON.toJSONString(request);
    }

    @Override
    public String useWenxinStreamTransformToGetAnswerAndExplanationAndKnowledge(String question) throws IOException {
        String prompt = promptTemplateService.renderPrompt(PromptKeys.MATH_ANSWER_EXPLANATION_KNOWLEDGE, Map.of(
                "question", String.valueOf(question)
        ));
        return connectWithBigModelStreamTransition(prompt);
    }

    @Override
    public Map<String, String> useWenxinStreamTransformToAnalyseAbilityByKnowledge(List<String> knowledge) throws IOException {

        String question = promptTemplateService.renderPrompt(PromptKeys.MATH_ABILITY_BY_KNOWLEDGE, Map.of(
                "knowledge", String.valueOf(knowledge)
        ));

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
        messages.add(dev.langchain4j.data.message.SystemMessage.from(promptTemplateService.getPrompt(PromptKeys.MATH_RAG_SYSTEM)));
        messages.add(dev.langchain4j.data.message.SystemMessage.from(promptTemplateService.getPrompt(PromptKeys.MATH_RAG_STEPS)));

        messages.add(UserMessage.from("上下文：\n" + context + "\n\n问题：" + query));

        dev.langchain4j.model.output.Response<AiMessage> resp = wenxinChatModel.generate(messages);
        return resp == null || resp.content() == null ? "" : resp.content().text();
    }

    @Override
    public List<String> useWenxinStreamTransformToAnalyseWrongType(String question, String content) throws IOException, JSONException {
        String sid = StpUtil.getLoginIdAsString();
        String description = studentService.getStudentBySid(sid).getDescription();

        String require = promptTemplateService.renderPrompt(PromptKeys.ANALYSIS_WRONG_TYPE_TEACHING_PLAN, Map.of(
                "description", String.valueOf(description),
                "question", String.valueOf(question),
                "content", String.valueOf(content)
        ));


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
        String sessionKey = sid + ":" + qidForChatHistory;
        BasicQuestion basicQuestion = new BasicQuestion();
        basicQuestion.setQid(qid);
        String question = String.valueOf(basicQuestionService.getQuestionTextByQid(basicQuestion));

        ChatSession currentSession = sessions.get(sessionKey);
        String displayContent = content;

        if (currentSession == null) {
            currentSession = new ChatSession();
            sessions.put(sessionKey, currentSession);
            content = promptTemplateService.renderPrompt(PromptKeys.CHAT_QUESTION_FIRST_TURN, Map.of(
                    "question", String.valueOf(question),
                    "content", String.valueOf(content)
            ));
        }

        ChatSession chatSession = currentSession;
        HashMap<String, String> user = new HashMap<>();
        user.put("role","user");
        user.put("content",content);
        user.put("displayUser",displayContent);
        chatSession.addMessage(user);
        String requestJson = constructRequestJson(1,0.95,0.8,1.0,true,new ArrayList<>(chatSession.getMessages()));

        //进行数据访问 返回String类型的数据
        StringBuffer answer=new StringBuffer();
        return deepSeekWebClient().post().uri(wenXinConfig.CHAT_COMPLETIONS_PATH)
                .bodyValue(requestJson)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(data -> Flux.fromIterable(extractStreamContents(data)))
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
                    chatSession.addMessage(assistant);

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
                    for (Map<String, String> message : chatSession.getMessages()) {
                        String role1 = message.get("role");
                        String content1 = message.get("content");
                        HashMap<String,String> temp = new HashMap<>();
                        temp.put(role1,content1);
                        if (message.containsKey("displayUser")) {
                            temp.put("displayUser", message.get("displayUser"));
                        }
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
        String sessionKey = sid + ":" + qidForChatHistory;
        BasicQuestion basicQuestion = new BasicQuestion();
        basicQuestion.setQid(qid);
        String question = String.valueOf(basicQuestionService.getQuestionTextByQid(basicQuestion));


        ChatSession currentSession = sessions.get(sessionKey);

        if (currentSession == null) {
            currentSession = new ChatSession();
            sessions.put(sessionKey, currentSession);
            content = promptTemplateService.renderPrompt(PromptKeys.CHAT_WRONG_ANSWER_FIRST_TURN, Map.of(
                    "question", String.valueOf(question),
                    "wrongText", String.valueOf(wrongText),
                    "wrongReason", String.valueOf(wrongReason),
                    "content", String.valueOf(content)
            ));
        }

        ChatSession chatSession = currentSession;
        HashMap<String, String> user = new HashMap<>();
        user.put("role","user");
        user.put("content",content);
        chatSession.addMessage(user);
        String requestJson = constructRequestJson(1,0.95,0.8,1.0,true,new ArrayList<>(chatSession.getMessages()));

        //进行数据访问 返回String类型的数据
        StringBuffer answer=new StringBuffer();
        return deepSeekWebClient().post().uri(wenXinConfig.CHAT_COMPLETIONS_PATH)
                .bodyValue(requestJson)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(data -> Flux.fromIterable(extractStreamContents(data)))
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
                    chatSession.addMessage(assistant);

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
                    for (Map<String, String> message : chatSession.getMessages()) {
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
        msg.put("content", String.valueOf(content));
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
