package org.musi.AI4Education.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.json.JSONException;
import org.musi.AI4Education.domain.*;
import org.musi.AI4Education.mapper.ChatHistoryMapper;
import org.musi.AI4Education.service.BasicQuestionService;
import org.musi.AI4Education.service.ChatGPTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;


@Service
public class ChatGPTServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatGPTService {
    private static String wavPath = "./output.wav";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BasicQuestionService basicQuestionService;

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
        String answer = "";
        try {
            ProcessBuilder pb;
                pb = new ProcessBuilder("/root/miniconda3/bin/python3.12", "/MusiProject/Python_API/TextAudioConversion/pcm_to_text.py");
            //pb = new ProcessBuilder("G:\\connectChatGPT\\venv\\Scripts\\python.exe", "G:\\green-farm\\src\\main\\java\\Python_API\\TextAudioConversion\\pcm_to_text.py", filePath);
            Process p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(),"gb2312"));
            String line;
            while ((line = in.readLine()) != null) {
                answer += line;
            }
            in.close();
            p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return answer;
    }

    @Override
    public Boolean getWavByText(String text) {
        String answer = "";
        try {
            ProcessBuilder pb;
            if (text == null) {
                pb = new ProcessBuilder("/root/miniconda3/bin/python3.12", "/MusiProject/Python_API/TextAudioConversion/text_to_wav.py");
                //pb = new ProcessBuilder("python", "G:\\greenfarm\\src\\main\\java\\Python_API\\TextAudioConversion\\text_to_wav.py", "你好, 欢迎使用文本转语音服务，接下来我将为你讲解数学题", wavPath);
            }else {
                pb = new ProcessBuilder("/root/miniconda3/bin/python3.12", "/MusiProject/Python_API/TextAudioConversion/text_to_wav.py");
               // pb = new ProcessBuilder("python", "G:\\greenfarm\\src\\main\\java\\Python_API\\TextAudioConversion\\text_to_wav.py", text, wavPath);
            }
            Process p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream(),"gb2312"));
            String line;
            while ((line = in.readLine()) != null) {
                answer += line;
            }
            in.close();
            p.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(answer.equals("Received binary data.Task finished successfully.")){
            return true;
        }
        return false;
    }
}
