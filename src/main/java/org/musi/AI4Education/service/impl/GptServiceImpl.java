package org.musi.AI4Education.service.impl;


import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.musi.AI4Education.domain.*;
import org.musi.AI4Education.prompt.PromptKeys;
import org.musi.AI4Education.service.BasicQuestionService;
import org.musi.AI4Education.service.PromptTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author aicnn.cn
 * @date 2023/2/13
 * @description: aicnn.cn
 **/
@Service
public class GptServiceImpl {

    private WebClient webClient;
    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private BasicQuestionService basicQuestionService;
    @Autowired
    private PromptTemplateService promptTemplateService;
    private Map<String, ChatSession> sessions = new HashMap<>(); // Store sessions using user IDs

    @Value("${qwen.api-key:sk-8da5d92cd1bd48839addaec3e03f8260}")
    private String qwenApiKey;

    @Value("${qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String qwenBaseUrl;

    @Value("${qwen.chat-completions-path:/chat/completions}")
    private String qwenChatCompletionsPath;

    @Value("${qwen.model:qwen-plus}")
    private String qwenModel;

    //服务器测试版本
    @PostConstruct
    public void postConstruct() {
        this.webClient = WebClient.builder()
                .baseUrl(qwenBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    private ChatSession loadChatSession(String sid, String qidForChatHistory, String collectionName) {
        ChatSession session = new ChatSession();
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("sid").is(sid),
                Criteria.where("qid").is(qidForChatHistory)
        ));
        Document history = mongoTemplate.findOne(query, Document.class, collectionName);
        if (history == null) {
            return session;
        }
        List<Document> messages = history.getList("wenxinChatHistory", Document.class);
        if (messages == null) {
            return session;
        }
        for (Document message : messages) {
            for (String role : List.of("user", "assistant")) {
                String content = message.getString(role);
                if (content != null) {
                    HashMap<String, String> item = new HashMap<>();
                    item.put("role", role);
                    item.put("content", content);
                    session.addMessage(item);
                }
            }
        }
        return session;
    }

    //请求stream的主题
    public Flux<AIAnswerDTO> doChatGPTStreamForInspiration(String sid,String qid, String question) {

        String qidForChatHistory = qid + "003";
        String sessionKey = sid + ":" + qidForChatHistory;
        //构建请求对象
        ChatRequestDTO chatRequestDTO = new ChatRequestDTO();
        chatRequestDTO.setModel(qwenModel);//设置模型
        chatRequestDTO.setStream(true);//设置流式返回

        // 直接尝试获取会话对象
        ChatSession session = sessions.get(sessionKey);
        //如果 Session 没有内容
        if (session == null) { // 如果获取的会话对象为空
            // 说明这是第一次创建
            session = loadChatSession(sid, qidForChatHistory, "inspirationChatHistory"); // 创建新的会话对象
            sessions.put(sessionKey, session); // 将新的会话对象放入 sessions 中

            if (session.getMessages().isEmpty()) {
                BasicQuestion basicQuestion = new BasicQuestion();
                basicQuestion.setQid(qid);
                String question_text = basicQuestionService.getQuestionTextByQid(basicQuestion);
                String wrongText = basicQuestionService.getQuestionWrongTextByQid(basicQuestion);
                if (wrongText.isBlank()) {
                    wrongText = "学生尚未上传自己的解题方法，请先围绕题目本身进行引导。";
                }

                Query query = new Query();
                query.addCriteria(Criteria.where("qid").is(qid));
                ConcreteQuestion concreteQuestion = mongoTemplate.findOne(query, ConcreteQuestion.class);
                String question_analysis = concreteQuestion == null ? "" : concreteQuestion.getQuestionAnalysis();

                question = promptTemplateService.renderPrompt(PromptKeys.STUDENT_INSPIRATION, Map.of(
                        "question", String.valueOf(question_text),
                        "questionAnalysis", String.valueOf(question_analysis),
                        "wrongText", String.valueOf(wrongText)
                ));
            }
        }


        //如果 Session 有内容
        ChatRequestDTO.ReqMessage message = new ChatRequestDTO.ReqMessage();//设置请求消息，在此可以加入自己的prompt
        message.setRole("user");//用户消息
        message.setContent(question);//用户请求内容

        //如果获得了会话对象，说明并不是第一次创建，则直接在Session里面添加接下来的问题
        //将用户的问题存入Session
        ArrayList<ChatRequestDTO.ReqMessage> messages = new ArrayList<>();

        //先获取之前的大模型聊天记录
        ChatSession session1 = sessions.getOrDefault(sessionKey, new ChatSession());

        List<HashMap<String, String>> messages0 = session1.getMessages();
        for (HashMap<String, String> message0 : messages0) {
            String role1 = message0.get("role");
            String content1 = message0.get("content");
            ChatRequestDTO.ReqMessage message1 = new ChatRequestDTO.ReqMessage();
            message1.setRole(role1);
            message1.setContent(content1);
            messages.add(message1);
        }

        //再将新的问题存入Session,并添加到请求里
        HashMap<String, String> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", question);
        session1.addMessage(msg);
        sessions.put(sessionKey, session1);

        // 请求 ChatGPT 请求头
        messages.add(message);
        chatRequestDTO.setMessages(messages);//设置请求消息

        //构建请求json
        String paramJson = JSONUtil.toJsonStr(chatRequestDTO);

        return this.webClient.post()
                .uri(qwenChatCompletionsPath)
                .header("Authorization", "Bearer " + qwenApiKey)
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)//设置流式响应
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(paramJson))
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(result -> handleWebClientResponseForInspiration(sid,qid,result));//接收到消息的处理方法

    }

    private Flux<AIAnswerDTO> handleWebClientResponseForInspiration(String sid,String qid,String resp) {

        String qidForContent = sid + ":" + qid + "000" + "003";
        String qidForChatHistory = qid + "003";
        String sessionKey = sid + ":" + qidForChatHistory;

        if (StrUtil.equals("[DONE]",resp)){//[DONE]是消息结束标识

            /*
            目的：将大模型的返回结果存入Session和数据库
             */

            //将大模型返回的消息存入Session
            //首先获取之前的Session，将大模型答案添加到Session中
            ChatSession session = sessions.getOrDefault(qidForContent, new ChatSession());
            String answer = session.getContent();

            ChatSession session1 = sessions.getOrDefault(sessionKey,new ChatSession());


            HashMap<String, String> msg1 = new HashMap<>();
            msg1.put("role", "assistant");
            msg1.put("content", answer);
            session1.addMessage(msg1);
            sessions.put(sessionKey, session1);


            //清空当前sesssion内容，为下一次存储做准备
            System.out.println("Answer: "+answer);
            session.ClearContent();


            //存入MongoDB
            //获取当前该用户的该题的聊天记录
            ChatSession session2 = sessions.getOrDefault(sessionKey, new ChatSession());
            List<HashMap<String, String>> messages1 = session2.getMessages();
            List<HashMap<String,String>> chatHistoryTemp = new ArrayList<>();
            for (HashMap<String, String> message : messages1) {
                String role1 = message.get("role");
                String content1 = message.get("content");
                System.out.println(role1 + ": " + content1);
                HashMap<String,String> temp = new HashMap<>();
                temp.put(role1,content1);
                chatHistoryTemp.add(temp);
            }


            // 组合多个查询条件，并在MongoDB中查询
            Criteria criteria1 = Criteria.where("sid").is(sid);
            Criteria criteria2 = Criteria.where("qid").is(qidForChatHistory);
            Criteria criteria = new Criteria().andOperator(criteria1, criteria2);
            Query query = new Query(criteria);
            List<InspirationChatHistory> result = mongoTemplate.find(query, InspirationChatHistory.class);


            if (result.isEmpty()) {
                System.out.println("查询结果为空");
                InspirationChatHistory chatHistory = new InspirationChatHistory();
                chatHistory.setQid(qidForChatHistory);
                chatHistory.setSid(sid);
                chatHistory.setWenxinChatHistory(chatHistoryTemp);
                mongoTemplate.insert(chatHistory);
            } else {
                System.out.println("查询结果不为空");
                Update update = new Update().set("wenxinChatHistory",chatHistoryTemp);
                mongoTemplate.updateFirst(query, update, "inspirationChatHistory");
            }


            return Flux.empty();
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(resp);
            AIAnswerDTO result = objectMapper.treeToValue(jsonNode, AIAnswerDTO.class);//将获得的结果转成对象
            if (CollUtil.size(result.getChoices())  > 0 && !Objects.isNull(result.getChoices().get(0)) &&
                    !StrUtil.isBlank(result.getChoices().get(0).delta.getError())){//判断是否有异常
                throw new RuntimeException(result.getChoices().get(0).delta.getError());
            }

            String dataPiece = result.getChoices().get(0).getDelta().getContent();

            // 获取会话对象,并存入信息
            ChatSession session = sessions.getOrDefault(qidForContent,new ChatSession());
            if(dataPiece!=null) {
                session.addContent(dataPiece);
                sessions.put(qidForContent, session); // 将新的会话对象放入 sessions 中
            }

            return Flux.just(result);//返回获得的结果
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public Flux<AIAnswerDTO> doChatGPTStreamForExplanation(String sid,String qid, String question,String studentCharactor) {

        String qidForChatHistory = qid + "004";
        String sessionKey = sid + ":" + qidForChatHistory;
        //构建请求对象
        ChatRequestDTO chatRequestDTO = new ChatRequestDTO();
        chatRequestDTO.setModel(qwenModel);//设置模型
        chatRequestDTO.setStream(true);//设置流式返回

        // 直接尝试获取会话对象
        ChatSession session = sessions.get(sessionKey);
        //如果 Session 没有内容
        if (session == null) { // 如果获取的会话对象为空
            // 说明这是第一次创建
            session = loadChatSession(sid, qidForChatHistory, "explanationChatHistory"); // 创建新的会话对象
            sessions.put(sessionKey, session); // 将新的会话对象放入 sessions 中

            if (session.getMessages().isEmpty()) {
                BasicQuestion basicQuestion = new BasicQuestion();
                basicQuestion.setQid(qid);
                String question_text = basicQuestionService.getQuestionTextByQid(basicQuestion);

                Query query = new Query();
                query.addCriteria(Criteria.where("qid").is(qid));
                ConcreteQuestion concreteQuestion = mongoTemplate.findOne(query, ConcreteQuestion.class);

                String question_analysis = concreteQuestion == null ? "" : concreteQuestion.getQuestionAnalysis();

                question = promptTemplateService.renderPrompt(PromptKeys.STUDENT_PERSONAL_EXPLANATION, Map.of(
                        "studentCharacter", String.valueOf(studentCharactor),
                        "question", String.valueOf(question_text),
                        "questionAnalysis", String.valueOf(question_analysis)
                ));
            }
        }

        //如果 Session 有内容
        ChatRequestDTO.ReqMessage message = new ChatRequestDTO.ReqMessage();//设置请求消息，在此可以加入自己的prompt
        message.setRole("user");//用户消息
        message.setContent(question);//用户请求内容

        //如果获得了会话对象，说明并不是第一次创建，则直接在Session里面添加接下来的问题
        //将用户的问题存入Session
        ArrayList<ChatRequestDTO.ReqMessage> messages = new ArrayList<>();


        //先获取之前的大模型聊天记录
        ChatSession session1 = sessions.getOrDefault(sessionKey, new ChatSession());

        List<HashMap<String, String>> messages0 = session1.getMessages();
        for (HashMap<String, String> message0 : messages0) {
            String role1 = message0.get("role");
            String content1 = message0.get("content");
            ChatRequestDTO.ReqMessage message1 = new ChatRequestDTO.ReqMessage();
            message1.setRole(role1);
            message1.setContent(content1);
            messages.add(message1);
        }

        //再将新的问题存入Session,并添加到请求里
        HashMap<String, String> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", question);
        session1.addMessage(msg);
        sessions.put(sessionKey, session1);

        // 请求 ChatGPT 请求头
        messages.add(message);
        chatRequestDTO.setMessages(messages);//设置请求消息

        //构建请求json
        String paramJson = JSONUtil.toJsonStr(chatRequestDTO);;

        return this.webClient.post()
                .uri(qwenChatCompletionsPath)
                .header("Authorization", "Bearer " + qwenApiKey)
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)//设置流式响应
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(paramJson))
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(result -> handleWebClientResponseForExplanation(sid,qid,result));//接收到消息的处理方法

    }

    private Flux<AIAnswerDTO> handleWebClientResponseForExplanation(String sid,String qid,String resp) {

        String qidForContent = sid + ":" + qid + "000" + "004";
        String qidForChatHistory = qid + "004";
        String sessionKey = sid + ":" + qidForChatHistory;

        if (StrUtil.equals("[DONE]",resp)){//[DONE]是消息结束标识

            /*
            目的：将大模型的返回结果存入Session和数据库
             */

            //将大模型返回的消息存入Session
            //首先获取之前的Session，将大模型答案添加到Session中
            ChatSession session = sessions.getOrDefault(qidForContent, new ChatSession());
            String answer = session.getContent();

            ChatSession session1 = sessions.getOrDefault(sessionKey,new ChatSession());


            HashMap<String, String> msg1 = new HashMap<>();
            msg1.put("role", "assistant");
            msg1.put("content", answer);
            session1.addMessage(msg1);
            sessions.put(sessionKey, session1);


            //清空当前sesssion内容，为下一次存储做准备
            System.out.println("Answer: "+answer);
            session.ClearContent();


            //存入MongoDB
            //获取当前该用户的该题的聊天记录
            ChatSession session2 = sessions.getOrDefault(sessionKey, new ChatSession());
            List<HashMap<String, String>> messages1 = session2.getMessages();
            List<HashMap<String,String>> chatHistoryTemp = new ArrayList<>();
            for (HashMap<String, String> message : messages1) {
                String role1 = message.get("role");
                String content1 = message.get("content");
                System.out.println(role1 + ": " + content1);
                HashMap<String,String> temp = new HashMap<>();
                temp.put(role1,content1);
                chatHistoryTemp.add(temp);
            }


            // 组合多个查询条件，并在MongoDB中查询
            Criteria criteria1 = Criteria.where("sid").is(sid);
            Criteria criteria2 = Criteria.where("qid").is(qidForChatHistory);
            Criteria criteria = new Criteria().andOperator(criteria1, criteria2);
            Query query = new Query(criteria);
            List<ExplanationChatHistory> result = mongoTemplate.find(query, ExplanationChatHistory.class);


            if (result.isEmpty()) {
                System.out.println("查询结果为空");
                ExplanationChatHistory chatHistory = new ExplanationChatHistory();
                chatHistory.setQid(qidForChatHistory);
                chatHistory.setSid(sid);
                chatHistory.setWenxinChatHistory(chatHistoryTemp);
                mongoTemplate.insert(chatHistory);
            } else {
                System.out.println("查询结果不为空");
                Update update = new Update().set("wenxinChatHistory",chatHistoryTemp);
                mongoTemplate.updateFirst(query, update, "explanationChatHistory");
            }


            return Flux.empty();
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(resp);
            AIAnswerDTO result = objectMapper.treeToValue(jsonNode, AIAnswerDTO.class);//将获得的结果转成对象
            if (CollUtil.size(result.getChoices())  > 0 && !Objects.isNull(result.getChoices().get(0)) &&
                    !StrUtil.isBlank(result.getChoices().get(0).delta.getError())){//判断是否有异常
                throw new RuntimeException(result.getChoices().get(0).delta.getError());
            }

            String dataPiece = result.getChoices().get(0).getDelta().getContent();

            // 获取会话对象,并存入信息
            ChatSession session = sessions.getOrDefault(qidForContent,new ChatSession());
            if(dataPiece!=null) {
                session.addContent(dataPiece);
                sessions.put(qidForContent, session); // 将新的会话对象放入 sessions 中
            }

            return Flux.just(result);//返回获得的结果
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public Flux<AIAnswerDTO> doChatGPTStreamForFeiman(String sid,String qid, String question) {

        String qidForChatHistory = qid + "005";
        String sessionKey = sid + ":" + qidForChatHistory;
        //构建请求对象
        ChatRequestDTO chatRequestDTO = new ChatRequestDTO();
        chatRequestDTO.setModel(qwenModel);//设置模型
        chatRequestDTO.setStream(true);//设置流式返回

        // 直接尝试获取会话对象
        ChatSession session = sessions.get(sessionKey);
        //如果 Session 没有内容
        if (session == null) { // 如果获取的会话对象为空
            // 说明这是第一次创建
            session = loadChatSession(sid, qidForChatHistory, "feimanChatHistory"); // 创建新的会话对象
            sessions.put(sessionKey, session); // 将新的会话对象放入 sessions 中

            if (session.getMessages().isEmpty()) {
                BasicQuestion basicQuestion = new BasicQuestion();
                basicQuestion.setQid(qid);
                String question_text = basicQuestionService.getQuestionTextByQid(basicQuestion);

                Query query = new Query();
                query.addCriteria(Criteria.where("qid").is(qid));
                ConcreteQuestion concreteQuestion = mongoTemplate.findOne(query, ConcreteQuestion.class);
                String question_analysis = concreteQuestion == null ? "" : concreteQuestion.getQuestionAnalysis();

                question = promptTemplateService.renderPrompt(PromptKeys.STUDENT_FEIMAN, Map.of(
                        "question", String.valueOf(question_text),
                        "questionAnalysis", String.valueOf(question_analysis)
                ));
            }
        }

        //如果 Session 有内容
        ChatRequestDTO.ReqMessage message = new ChatRequestDTO.ReqMessage();//设置请求消息，在此可以加入自己的prompt
        message.setRole("user");//用户消息
        message.setContent(question);//用户请求内容

        //如果获得了会话对象，说明并不是第一次创建，则直接在Session里面添加接下来的问题
        //将用户的问题存入Session
        ArrayList<ChatRequestDTO.ReqMessage> messages = new ArrayList<>();


        //先获取之前的大模型聊天记录
        ChatSession session1 = sessions.getOrDefault(sessionKey, new ChatSession());

        List<HashMap<String, String>> messages0 = session1.getMessages();
        for (HashMap<String, String> message0 : messages0) {
            String role1 = message0.get("role");
            String content1 = message0.get("content");
            ChatRequestDTO.ReqMessage message1 = new ChatRequestDTO.ReqMessage();
            message1.setRole(role1);
            message1.setContent(content1);
            messages.add(message1);
        }

        //再将新的问题存入Session,并添加到请求里
        HashMap<String, String> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", question);
        session1.addMessage(msg);
        sessions.put(sessionKey, session1);

        // 请求 ChatGPT 请求头
        messages.add(message);
        chatRequestDTO.setMessages(messages);//设置请求消息

        //构建请求json
        String paramJson = JSONUtil.toJsonStr(chatRequestDTO);

        return this.webClient.post()
                .uri(qwenChatCompletionsPath)
                .header("Authorization", "Bearer " + qwenApiKey)
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)//设置流式响应
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(paramJson))
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(result -> handleWebClientResponseForFeiman(sid,qid,result));//接收到消息的处理方法

    }

    private Flux<AIAnswerDTO> handleWebClientResponseForFeiman(String sid,String qid,String resp) {

        String qidForContent = sid + ":" + qid + "000" + "005";
        String qidForChatHistory = qid + "005";
        String sessionKey = sid + ":" + qidForChatHistory;

        if (StrUtil.equals("[DONE]",resp)){//[DONE]是消息结束标识

            /*
            目的：将大模型的返回结果存入Session和数据库
             */

            //将大模型返回的消息存入Session
            //首先获取之前的Session，将大模型答案添加到Session中
            ChatSession session = sessions.getOrDefault(qidForContent, new ChatSession());
            String answer = session.getContent();

            ChatSession session1 = sessions.getOrDefault(sessionKey,new ChatSession());


            HashMap<String, String> msg1 = new HashMap<>();
            msg1.put("role", "assistant");
            msg1.put("content", answer);
            session1.addMessage(msg1);
            sessions.put(sessionKey, session1);


            //清空当前sesssion内容，为下一次存储做准备
            System.out.println("Answer: "+answer);
            session.ClearContent();


            //存入MongoDB
            //获取当前该用户的该题的聊天记录
            ChatSession session2 = sessions.getOrDefault(sessionKey, new ChatSession());
            List<HashMap<String, String>> messages1 = session2.getMessages();
            List<HashMap<String,String>> chatHistoryTemp = new ArrayList<>();
            for (HashMap<String, String> message : messages1) {
                String role1 = message.get("role");
                String content1 = message.get("content");
                System.out.println(role1 + ": " + content1);
                HashMap<String,String> temp = new HashMap<>();
                temp.put(role1,content1);
                chatHistoryTemp.add(temp);
            }


            // 组合多个查询条件，并在MongoDB中查询
            Criteria criteria1 = Criteria.where("sid").is(sid);
            Criteria criteria2 = Criteria.where("qid").is(qidForChatHistory);
            Criteria criteria = new Criteria().andOperator(criteria1, criteria2);
            Query query = new Query(criteria);
            List<FeimanChatHistory> result = mongoTemplate.find(query, FeimanChatHistory.class);


            if (result.isEmpty()) {
                System.out.println("查询结果为空");
                FeimanChatHistory chatHistory = new FeimanChatHistory();
                chatHistory.setQid(qidForChatHistory);
                chatHistory.setSid(sid);
                chatHistory.setWenxinChatHistory(chatHistoryTemp);
                mongoTemplate.insert(chatHistory);
            } else {
                System.out.println("查询结果不为空");
                Update update = new Update().set("wenxinChatHistory",chatHistoryTemp);
                mongoTemplate.updateFirst(query, update, "feimanChatHistory");
            }


            return Flux.empty();
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(resp);
            AIAnswerDTO result = objectMapper.treeToValue(jsonNode, AIAnswerDTO.class);//将获得的结果转成对象
            if (CollUtil.size(result.getChoices())  > 0 && !Objects.isNull(result.getChoices().get(0)) &&
                    !StrUtil.isBlank(result.getChoices().get(0).delta.getError())){//判断是否有异常
                throw new RuntimeException(result.getChoices().get(0).delta.getError());
            }

            String dataPiece = result.getChoices().get(0).getDelta().getContent();

            // 获取会话对象,并存入信息
            ChatSession session = sessions.getOrDefault(qidForContent,new ChatSession());
            if(dataPiece!=null) {
                session.addContent(dataPiece);
                sessions.put(qidForContent, session); // 将新的会话对象放入 sessions 中
            }

            return Flux.just(result);//返回获得的结果
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


}
