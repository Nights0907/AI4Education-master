package org.musi.AI4Education.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.musi.AI4Education.config.Wen_XinConfig;
import org.musi.AI4Education.domain.BasicQuestion;
import org.musi.AI4Education.service.ConcreteQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@Slf4j
public class TestController {
    @Resource
    private Wen_XinConfig wenXinConfig;

    @Autowired
    private ConcreteQuestionService concreteQuestionService;

    //历史对话，需要按照user,assistant
    List<Map<String,String>> messages = new ArrayList<>();

    /**
     * 流式回答
     * @return
     */
    @PostMapping("/test2")
    public String test2(String question){
        OkHttpClient client = new OkHttpClient();

        HashMap<String, String> user = new HashMap<>();
        user.put("role","user");
        user.put("content",question);
        messages.add(user);
        String requestJson = constructRequestJson(1,0.95,0.8,1.0,true,messages);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestJson);
        Request request = new Request.Builder()
                .url(wenXinConfig.ERNIE_Bot_4_0_URL + "?access_token=" + wenXinConfig.flushAccessToken())
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();

        StringBuilder answer = new StringBuilder();
        // 发起异步请求
        try {
            Response response = client.newCall(request).execute();
            // 检查响应是否成功
            if (response.isSuccessful()) {
                // 获取响应流
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody != null) {
                        InputStream inputStream = responseBody.byteStream();
                        // 以流的方式处理响应内容，输出到控制台
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            // 在控制台输出每个数据块
                            System.out.write(buffer, 0, bytesRead);
                            //将结果汇总起来
                            answer.append(new String(buffer, 0, bytesRead));
                        }
                    }
                }
            } else {
                System.out.println("Unexpected code " + response);
            }

        } catch (IOException e) {
            log.error("流式请求出错");
            throw new RuntimeException(e);
        }
        //将回复的内容添加到消息中
        HashMap<String, String> assistant = new HashMap<>();
        assistant.put("role","assistant");
        assistant.put("content","");
        //取出我们需要的内容,也就是result部分
        String[] answerArray = answer.toString().split("data: ");
        for (int i=1;i<answerArray.length;++i) {
            answerArray[i] = answerArray[i].substring(0,answerArray[i].length() - 2);
            assistant.put("content",assistant.get("content") + JSON.parseObject(answerArray[i]).get("result"));
        }
        messages.add(assistant);
        return assistant.get("content");
    }

    /**
     * 构造请求的请求参数
     * @param userId
     * @param temperature
     * @param topP
     * @param penaltyScore
     * @param messages
     * @return
     */
    public String constructRequestJson(Integer userId,
                                       Double temperature,
                                       Double topP,
                                       Double penaltyScore,
                                       boolean stream,
                                       List<Map<String, String>> messages) {
        Map<String,Object> request = new HashMap<>();
        request.put("user_id",userId.toString());
        request.put("temperature",temperature);
        request.put("top_p",topP);
        request.put("penalty_score",penaltyScore);
        request.put("stream",stream);
        request.put("messages",messages);
        System.out.println(JSON.toJSONString(request));
        return JSON.toJSONString(request);
    }

    private final ConcurrentMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @PostMapping("/stream")
    public void test(@RequestParam String content) throws IOException{
        concreteQuestionService.test(content);
    }

}
