package org.musi.AI4Education.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.musi.AI4Education.domain.BasicQuestion;
import org.musi.AI4Education.domain.ChatHistory;
import org.musi.AI4Education.domain.StudentProfile;
import com.fasterxml.jackson.databind.JsonNode;
import org.musi.AI4Education.domain.*;
import org.musi.AI4Education.mapper.BasicQuestionMapper;
import org.musi.AI4Education.mapper.StudentProfileMapper;
import org.musi.AI4Education.service.BasicQuestionService;
import org.musi.AI4Education.service.ChatGPTService;
import org.musi.AI4Education.service.ConcreteQuestionService;
import org.musi.AI4Education.service.StudentProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;


import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class StudentProfileServiceImpl extends ServiceImpl<StudentProfileMapper, StudentProfile> implements StudentProfileService {
    private WebClient webClient;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private StudentProfileMapper studentProfileMapper;
    @Autowired
    private BasicQuestionMapper basicQuestionMapper;
    @Autowired
    private ChatGPTService chatGPTservice;
    @Autowired
    private ConcreteQuestionService concreteQuestionService;

    private Map<String, ChatSession> sessions = new HashMap<>(); // Store sessions using user IDs
    @Autowired
    private BasicQuestionService basicQuestionService;
    private String OPENAI_KEY = "sk-eb3b86139e574719aa5aed8dc1348cc7";;


    //服务器测试版本
    @PostConstruct
    public void postConstruct() {
        this.webClient = WebClient.builder()//创建webflux的client
                //.baseUrl("https://gateway.ai.cloudflare.com/v1/323f46a86f2c41a6c889c57cccac62fb/musi/openai")//填写对应的api地址
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")//填写对应的api地址
                .defaultHeader("Content-Type", "application/json")//设置默认请求类型
                .build();
    }


    @Override
    public StudentProfile getStudentProfileBySid(String sid) {

        Criteria criteria = Criteria.where("sid").is(sid);
        Query query = new Query(criteria);
        StudentProfile result = mongoTemplate.findOne(query, StudentProfile.class);
        return result;

    }

    @Override
    public StudentProfile createStudentProfile(StudentProfile studentProfile) {
        mongoTemplate.insert(studentProfile);
        return studentProfile;
    }

    @Override
    public StudentProfile updateStudentProfile(StudentProfile studentProfile) {
        Query query = new Query(Criteria.where("sid").is(studentProfile.getSid()));
        Update update = new Update();
        update.set("abilityPointList", studentProfile.getAbilityPointList());
        update.set("knowledgePointList",studentProfile.getKnowledgePointList());
        mongoTemplate.updateFirst(query, update, "studentProfile"); // 你的文档类名替换为实际的类名
        return studentProfile;
    }


    @Override
    public Map<String, Long> countQuestionPerDay() {
        String sid = StpUtil.getLoginIdAsString();
        LocalDate endDate = LocalDate.now(); // 结束日期为今天
        LocalDate startDate = endDate.minusDays(14); // 开始日期为最近15天内的前一天

        QueryWrapper<BasicQuestion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sid", sid);
        queryWrapper.between("date", Date.valueOf(startDate), Date.valueOf(endDate)); // 查询条件为日期在最近15天内的数据

        List<BasicQuestion> resultList = basicQuestionMapper.selectList(queryWrapper);

        // 使用Java8的流式操作进行统计每天的搜题量
        Map<String, Long> countPerDayMap = resultList.stream()
                .collect(Collectors.groupingBy(
                        entity -> entity.getDate().toString(),
                        TreeMap::new, // 使用 TreeMap 保证按日期排序
                        Collectors.counting()
                ));

        // 补全缺少的日期，并设置对应日期的搜题量为0
        for (int i = 0; i < 15; i++) {
            LocalDate date = endDate.minusDays(i);
            countPerDayMap.putIfAbsent(String.valueOf(Date.valueOf(date)).substring(0,10), 0L);
        }

        Map<String, Long> sortedMap = new TreeMap<>(countPerDayMap);

        return sortedMap;
    }

    //更新学生档案中个性偏好部分，无法得到返回值
    @Override
    public Flux<AIAnswerDTO> updateCharacterPointByQidAndSid(StudentProfile studentProfile) {
        String sid = studentProfile.getSid();
        List<String> qidList = basicQuestionService.getQidListBySid(sid);
        //String qid = "1711249417288";
        String qid = qidList.get(qidList.size()-1);
        String qidForChatHistory = qid + "008";
        //获得历史聊天记录
        ExplanationChatHistory explanationChatHistory = chatGPTservice.getExplanationChatHistoryByQid(qid);
        FeimanChatHistory feimanChatHistory = chatGPTservice.getFeimanChatHistoryByQid(qid);
        InspirationChatHistory inspirationChatHistory = chatGPTservice.getInspirationChatHistoryByQid(qid);
        List<HashMap<String, String>> mergedChatHistories = mergeChatHistories(explanationChatHistory, feimanChatHistory, inspirationChatHistory);

        //构建请求对象
        ChatRequestDTO chatRequestDTO = new ChatRequestDTO();
        chatRequestDTO.setModel("qwen-plus");//设置模型
        chatRequestDTO.setStream(true);//设置流式返回
        // 直接尝试获取会话对象
        ChatSession session = sessions.get(qidForChatHistory);
        String question = "";
        if(session == null) {
            // 说明这是第一次创建
            session = new ChatSession(); // 创建新的会话对象
            sessions.put(qidForChatHistory, session); // 将新的会话对象放入 sessions 中
        }

        String first = "";

        String filePath = "E:\\AI4Education\\src\\main\\java\\Python_API\\PersonalCharacter\\prompt.txt";

        try {
            // 创建FileReader对象
            FileReader fileReader = new FileReader(filePath);
            // 创建BufferedReader对象
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            // 读取文件内容
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                first = first + line;
            }
            System.out.println(first);
            // 关闭BufferedReader
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //组合
        question = first + "下面是对话记录：" + mergedChatHistories;

        ChatRequestDTO.ReqMessage message = new ChatRequestDTO.ReqMessage();//设置请求消息，在此可以加入自己的prompt
        message.setRole("user");//用户消息
        message.setContent(question);//用户请求内容

        ArrayList<ChatRequestDTO.ReqMessage> messages = new ArrayList<>();
        //先获取之前的大模型聊天记录
        ChatSession session1 = sessions.getOrDefault(qidForChatHistory, new ChatSession());

        //  直接放入新请求
        HashMap<String, String> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", question);
        session1.addMessage(msg);
        sessions.put(qidForChatHistory, session1);

        // 请求 ChatGPT 请求头
        messages.add(message);
        chatRequestDTO.setMessages(messages);//设置请求消息

        //构建请求json
        String paramJson = JSONUtil.toJsonStr(chatRequestDTO);
        System.out.println(paramJson);

        return this.webClient.post()
                .uri("/chat/completions")//请求uri
                .header("Authorization", OPENAI_KEY)//设置成自己的key，获得key的方式可以在下文查看
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)//设置流式响应
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(paramJson))
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(result -> handleWebClientResponseForupdateCharacterPoint(sid, qid, result));//接收到消息的处理方法
    }

    private Flux<AIAnswerDTO> handleWebClientResponseForupdateCharacterPoint(String sid, String qid, String resp) {
        String qidForContent = qid + "000" + "008";
        String qidForChatHistory = qid + "008";


        System.out.println("this is feedback");
        System.out.println(resp);

        if (StrUtil.equals("[DONE]",resp)){//[DONE]是消息结束标识
            System.out.println(resp);
            /*
            目的：将大模型的返回结果存入数据库
             */
            //将大模型返回的消息存入Session
            //首先获取之前的Session，将大模型答案添加到Session中
            ChatSession session = sessions.get(qidForContent);
            String answer = session.getContent();

            ChatSession session1 = sessions.getOrDefault(qidForChatHistory,new ChatSession());

            //获取之前所有的历史记录
            HashMap<String, String> msg1 = new HashMap<>();
            List<HashMap<String, String>> messages = session1.getMessages();
            for (HashMap<String, String> message : messages) {
                String role1 = message.get("role");
                String content1 = message.get("content");
                msg1.put(role1, content1);
            }
            //将新的大模型结果存入Session
            msg1.put("role", "assistant");
            msg1.put("content", answer);
            session1.addMessage(msg1);
            sessions.put(qidForChatHistory, session1);

            //清空当前sesssion内容，为下一次存储做准备
            System.out.println("Answer: "+answer);
            session.ClearContent();

            List<CharacterPoint> characterPointList = new ArrayList<>();

            //处理字符串
            String[] parts = answer.split("。");

            for (String part : parts) {
                if (!part.isEmpty()) { // 跳过空字符串
                    String title = part.split("：")[0]; // 提取type
                    String content = part.substring(part.indexOf("：") + 1); // 提取description
                    title = title.substring(2);
                    System.out.println("type：" + title);
                    System.out.println("内容：" + content);

                    CharacterPoint characterPoint = new CharacterPoint();
                    characterPoint.setType(title);
                    characterPoint.setDescription(content);
                    characterPoint.setLatestDate(new java.util.Date());
                    characterPointList.add(characterPoint);
                }
            }


            //存入MongoDB

//            // 组合多个查询条件，并在MongoDB中查询
//            Criteria criteria = Criteria.where("sid").is(sid);
//            Query query = new Query(criteria);
//            List<StudentProfile> result = mongoTemplate.find(query, StudentProfile.class);
//
//            if (result.isEmpty()) {
//                System.out.println("查询结果为空");
//                StudentProfile studentProfile = new StudentProfile();
//                studentProfile.setSid(sid);
//                mongoTemplate.insert(characterPointList);
//            } else {
//                System.out.println("查询结果不为空");
//                Update update = new Update().set("characterPointList", mergeCharacterPoint(characterPointList, result.get(0).getCharacterPointList()));
//                mongoTemplate.updateFirst(query, update, "StudentProfile");
//            }

            Query query_1 = new Query(Criteria.where("sid").is(sid));
            Update update = new Update();
            update.set("characterPointList",characterPointList);
            mongoTemplate.updateFirst(query_1, update, "studentProfile"); // 你的文档类名替换为实际的类名

            return Flux.empty();
        }

        return Flux.empty();
    }

    @Override
    public void useWenxinUpdateCharacterPointByQidAndSid(StudentProfile studentProfile) throws IOException {
        String sid = studentProfile.getSid();
        List<String> qidList = basicQuestionService.getQidListBySid(sid);
        String qid = qidList.get(qidList.size()-1);
        //获得历史聊天记录
        ExplanationChatHistory explanationChatHistory = chatGPTservice.getExplanationChatHistoryByQid(qid);
        FeimanChatHistory feimanChatHistory = chatGPTservice.getFeimanChatHistoryByQid(qid);
        InspirationChatHistory inspirationChatHistory = chatGPTservice.getInspirationChatHistoryByQid(qid);
        List<HashMap<String, String>> mergedChatHistories = mergeChatHistories(explanationChatHistory, feimanChatHistory, inspirationChatHistory);

        String question = "";
        String first = "";

        //路径需要修改
        String filePath = "E:\\projects\\AI4Education-master\\src\\main\\java\\Python_API\\PersonalCharactor\\prompt.txt";

        try {
            // 创建FileReader对象
            FileReader fileReader = new FileReader(filePath);
            // 创建BufferedReader对象
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            // 读取文件内容
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                first = first + line;
            }
            System.out.println(first);
            // 关闭BufferedReader
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //组合
        question = first + "下面是对话记录：" + mergedChatHistories + "\n请只返回如<output>那样的内容,不用带上<output>";
        String result = concreteQuestionService.connectWithBigModelStreamTransition(question);

        // 处理字符串
        List<CharacterPoint> characterPointList = new ArrayList<>();

        Pattern pattern = Pattern.compile("\\[(.*?):(.*?)\\]");
        Matcher matcher = pattern.matcher(result);

        ArrayList<String> concepts = new ArrayList<>();
        ArrayList<String> descriptions = new ArrayList<>();
        CharacterPoint characterPoint = new CharacterPoint();
        while (matcher.find()) {
            concepts.add(matcher.group(1));
            descriptions.add(matcher.group(2));
        }
        // 输出提取到的概念和描述
        for (int i = 0; i < concepts.size(); i++) {
            System.out.println("概念：" + concepts.get(i));
            System.out.println("描述：" + descriptions.get(i));
            characterPoint.setType(concepts.get(i));
            characterPoint.setDescription(descriptions.get(i));
            characterPointList.add(characterPoint);
        }

        //存入MongoDB
        Query query_1 = new Query(Criteria.where("sid").is(sid));
        Update update = new Update();
        update.set("characterPointList",characterPointList);
        mongoTemplate.updateFirst(query_1, update, "studentProfile"); // 你的文档类名替换为实际的类名
    }


    private List<CharacterPoint> mergeCharacterPoint(
            List<CharacterPoint> characterPointList,
            List<CharacterPoint> characterPointList1) {
        List<CharacterPoint> result = new ArrayList<>();
        result.addAll(characterPointList);
        for(CharacterPoint oldCharacterPoint : characterPointList1){
            for(CharacterPoint newCharacterPoint : characterPointList){
                if(!oldCharacterPoint.getType().equals(newCharacterPoint.getType())){
                    result.add(oldCharacterPoint);
                }
            }
        }
        return result;
    }


    //    合并聊天记录
    private static List<HashMap<String, String>> mergeChatHistories(
            ExplanationChatHistory explanationChatHistory,
            FeimanChatHistory feimanChatHistory,
            InspirationChatHistory inspirationChatHistory) {

        List<HashMap<String, String>> mergedChatHistory = new ArrayList<>();
        // 检查每个列表是否为 null，并相应地处理
        if (explanationChatHistory != null && explanationChatHistory.getWenxinChatHistory() != null) {
            mergedChatHistory.addAll(explanationChatHistory.getWenxinChatHistory());
        }
        if (feimanChatHistory != null && feimanChatHistory.getWenxinChatHistory() != null) {
            mergedChatHistory.addAll(feimanChatHistory.getWenxinChatHistory());
        }
        if (inspirationChatHistory != null && inspirationChatHistory.getWenxinChatHistory() != null) {
            mergedChatHistory.addAll(inspirationChatHistory.getWenxinChatHistory());
        }

        return mergedChatHistory;
    }

    //获取个性偏好
    public List<CharacterPoint> getCharacterPointByQidAndSid(StudentProfile studentProfile){

        QueryWrapper<StudentProfile> wrapper = new QueryWrapper<>();

        wrapper.eq("sid",studentProfile.getSid());
//        wrapper.eq("type",studentProfile.getType());

        StudentProfile studentProfileTemp = studentProfileMapper.selectOne(wrapper);

        if(studentProfileTemp == null){
            return null;
        }

        return studentProfileTemp.getCharacterPointList();
    }

    @Override
    public Map<String, Object> getStudentProfileInformation(String sid){

        Criteria criteria = Criteria.where("sid").is(sid);
        Query query = new Query(criteria);
        StudentProfile studentProfile = mongoTemplate.findOne(query, StudentProfile.class);

        // 处理知识点
        List<KnowledgePoint> knowledgePointList = studentProfile.getKnowledgePointList();
        List<HashMap<String,String>> knowledge_weight = new ArrayList<>();

        for(KnowledgePoint knowledgePoint:knowledgePointList){
            HashMap<String,String> temp = new HashMap<>();
            int times = knowledgePoint.getTimes();
            String teachingMethod = "";
            if(times <= 3){
                teachingMethod = "该知识点掌握程度高，只需要给出粗略的教学";
            }else if(times > 3&& times < 5){
                teachingMethod = "该知识点掌握程度中等，需要比较详细地给出知识点的拆解与教学";
            }else{
                teachingMethod = "该知识点掌握程度低，知识点拆解与教学尽可能地做到最详细";
            }
            temp.put(knowledgePoint.getType(),teachingMethod);
            knowledge_weight.add(temp);
        }

        // 处理核心能力素养
        List<AbilityPoint> abilityPointList = studentProfile.getAbilityPointList();
        List<HashMap<String,String>> ability_weight = new ArrayList<>();
        String[] abilities = {"数学抽象", "数学建模", "数据分析", "直观想象", "逻辑推理", "数学运算"};
        int[] ability_weight_int_nums = new int[6];

        for(AbilityPoint abilityPoint : abilityPointList){
            if(abilityPoint.getType().equals("数学抽象")){
                ability_weight_int_nums[0] += abilityPoint.getTimes();
            }else if(abilityPoint.getType().equals("数学建模")){
                ability_weight_int_nums[1] += abilityPoint.getTimes();
            }else if(abilityPoint.getType().equals("数据分析")){
                ability_weight_int_nums[2] += abilityPoint.getTimes();
            }else if(abilityPoint.getType().equals("直观想象")){
                ability_weight_int_nums[3] += abilityPoint.getTimes();
            }else if(abilityPoint.getType().equals("逻辑推理")){
                ability_weight_int_nums[4] += abilityPoint.getTimes();
            }else{
                ability_weight_int_nums[5] += abilityPoint.getTimes();
            }
        }

        int index = 0;
        for(String ability :abilities){
            HashMap<String,String> temp = new HashMap<>();
            int times = ability_weight_int_nums[index++];
            String teachingMethod = "";
            if(times <= 3){
                teachingMethod = "该核心素养掌握程度高，只需要给出粗略的教学";
            }else if(times > 3&& times < 5){
                teachingMethod = "该核心素养掌握程度中等，需要比较详细地进行教学";
            }else{
                teachingMethod = "该核心素养掌握程度低，教学尽可能地做到最详细";
            }
            temp.put(ability,teachingMethod);
            ability_weight.add(temp);
        }


//        JSONArray jsonArray1 = new JSONArray(Collections.singletonList(knowledge_weight));
//        JSONArray jsonArray2 = new JSONArray(Collections.singletonList(ability_weight));

//        JSONObject resultJson = new JSONObject();
//        resultJson.put("knowledge_weight", jsonArray1);
//        resultJson.put("ability_weight", jsonArray2);

        Map<String, Object> data = new HashMap<>();
        data.put("knowledge_weight",knowledge_weight);
        data.put("ability_weight",ability_weight);


        return data;
    }
}
