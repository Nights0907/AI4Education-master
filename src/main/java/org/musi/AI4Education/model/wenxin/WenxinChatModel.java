package org.musi.AI4Education.model.wenxin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.output.Response;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.musi.AI4Education.config.Wen_XinConfig;

import java.io.IOException;
import java.util.List;

/**
 * Minimal ChatModel adapter for Baidu ERNIE (Wenxin) chat/completions endpoint.
 */
public class WenxinChatModel {

    private static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient httpClient;
    private final Wen_XinConfig wenXinConfig;
    public WenxinChatModel(OkHttpClient httpClient,
                           Wen_XinConfig wenXinConfig) {
        this.httpClient = httpClient;
        this.wenXinConfig = wenXinConfig;
    }

    public Response<AiMessage> generate(List<ChatMessage> messages) {
        JSONObject payload = new JSONObject();
        JSONArray msgArr = new JSONArray();
        StringBuilder sb = new StringBuilder();
        if (messages != null) {
            for (ChatMessage m : messages) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(String.valueOf(m));
            }
        }
        JSONObject one = new JSONObject();
        one.put("role", "user");
        one.put("content", sb.toString());
        msgArr.add(one);
        payload.put("messages", msgArr);

        // optional sensible defaults
        payload.put("temperature", 0.7);
        payload.put("top_p", 0.9);

        String url = wenXinConfig.ERNIE_Bot_4_0_URL + "?access_token=" + wenXinConfig.flushAccessToken();

        try {

            RequestBody body = RequestBody.create(JSON, payload.toJSONString());
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            okhttp3.Response resp = httpClient.newCall(request).execute();
            if (!resp.isSuccessful()) {
                String err = safeBody(resp.body());
                return Response.from(AiMessage.from(""));
            }
            String raw = safeBody(resp.body());
            JSONObject root = JSONObject.parseObject(raw);
            String result = root.getString("result");
            AiMessage aiMessage = AiMessage.from(result == null ? "" : result);

            return Response.from(aiMessage);
        } catch (Exception e) {
            return Response.from(AiMessage.from(""));
        }
    }

    // Provide a convenience overload in case callers use single-turn APIs
    public Response<AiMessage> generate(String userMessage) {
        JSONObject payload = new JSONObject();
        JSONArray msgArr = new JSONArray();
        JSONObject one = new JSONObject();
        one.put("role", "user");
        one.put("content", userMessage == null ? "" : userMessage);
        msgArr.add(one);
        payload.put("messages", msgArr);

        payload.put("temperature", 0.7);
        payload.put("top_p", 0.9);

        String url = wenXinConfig.ERNIE_Bot_4_0_URL + "?access_token=" + wenXinConfig.flushAccessToken();
        try {
            RequestBody body = RequestBody.create(JSON, payload.toJSONString());
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            okhttp3.Response resp = httpClient.newCall(request).execute();
            if (!resp.isSuccessful()) {
                safeBody(resp.body());
                return Response.from(AiMessage.from(""));
            }
            String raw = safeBody(resp.body());
            JSONObject root = JSONObject.parseObject(raw);
            String result = root.getString("result");
            AiMessage aiMessage = AiMessage.from(result == null ? "" : result);
            return Response.from(aiMessage);
        } catch (Exception e) {
            return Response.from(AiMessage.from(""));
        }
    }

    private static String safeBody(ResponseBody body) throws IOException {
        if (body == null) return "";
        try {
            return body.string();
        } finally {
            body.close();
        }
    }
}


