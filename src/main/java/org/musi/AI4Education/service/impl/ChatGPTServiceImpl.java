package org.musi.AI4Education.service.impl;

import org.musi.AI4Education.domain.chat.ChatHistory;
import org.musi.AI4Education.domain.chat.ChatSession;
import org.musi.AI4Education.domain.chat.ExplanationChatHistory;
import org.musi.AI4Education.domain.chat.FeimanChatHistory;
import org.musi.AI4Education.domain.chat.InspirationChatHistory;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.json.JSONException;
import org.musi.AI4Education.mapper.ChatHistoryMapper;
import org.musi.AI4Education.service.BasicQuestionService;
import org.musi.AI4Education.service.ChatGPTService;
import org.musi.AI4Education.utils.PythonProcessRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;


@Service
public class ChatGPTServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatGPTService {
    private static String wavPath = "./output.wav";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BasicQuestionService basicQuestionService;

    @Autowired
    private PythonProcessRunner pythonProcessRunner;

    private Map<String, ChatSession> sessions = new HashMap<>(); // Store sessions using user IDs

    @Override
    public InspirationChatHistory getInspirationChatHistoryByQid(String qid) {
        Criteria criteria1 = Criteria.where("sid").is(StpUtil.getLoginIdAsString());
        Criteria criteria2 = Criteria.where("qid").is(qid+"003");
        // 组合多个查询条件
        Criteria criteria = new Criteria().andOperator(criteria1, criteria2);
        // 创建查询对象
        Query query = new Query(criteria);
        InspirationChatHistory result = mongoTemplate.findOne(query, InspirationChatHistory.class);
        return result;
    }

    @Override
    public ExplanationChatHistory getExplanationChatHistoryByQid(String qid) {
        Criteria criteria1 = Criteria.where("sid").is(StpUtil.getLoginIdAsString());
        Criteria criteria2 = Criteria.where("qid").is(qid+"004");
        // 组合多个查询条件
        Criteria criteria = new Criteria().andOperator(criteria1, criteria2);
        // 创建查询对象
        Query query = new Query(criteria);
        ExplanationChatHistory result = mongoTemplate.findOne(query, ExplanationChatHistory.class);
        return result;
    }

    @Override
    public FeimanChatHistory getFeimanChatHistoryByQid(String qid) {
        Criteria criteria1 = Criteria.where("sid").is(StpUtil.getLoginIdAsString());
        Criteria criteria2 = Criteria.where("qid").is(qid+"005");
        // 组合多个查询条件
        Criteria criteria = new Criteria().andOperator(criteria1, criteria2);
        // 创建查询对象
        Query query = new Query(criteria);
        FeimanChatHistory result = mongoTemplate.findOne(query, FeimanChatHistory.class);
        return result;
    }

    @Override
    public String getTextByPcm(String filePath) {
        try {
            return pythonProcessRunner.runScript(
                    "src/main/java/Python_API/TextAudioConversion/pcm_to_text.py",
                    Charset.forName("GB2312"),
                    filePath
            ).trim();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("语音识别失败", e);
        }
    }

    @Override
    public Boolean getWavByText(String text) {
        String inputText = text == null ? "你好, 欢迎使用文本转语音服务，接下来我将为你讲解数学题" : text;
        wavPath = "./output-" + UUID.randomUUID() + ".wav";
        try {
            String answer = pythonProcessRunner.runScript(
                    "src/main/java/Python_API/TextAudioConversion/text_to_wav.py",
                    Charset.forName("GB2312"),
                    inputText,
                    wavPath
            );
            return answer.contains("Task finished successfully.");
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("文本转语音失败", e);
        }
    }
}
