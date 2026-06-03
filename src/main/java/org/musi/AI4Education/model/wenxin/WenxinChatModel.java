package org.musi.AI4Education.model.wenxin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.musi.AI4Education.config.Wen_XinConfig;

import java.util.List;

public class WenxinChatModel {

    private static final MediaType JSON = MediaType.parse("application/json");

    private final OkHttpClient httpClient;
    private final Wen_XinConfig wenXinConfig;

    public WenxinChatModel(OkHttpClient httpClient, Wen_XinConfig wenXinConfig) {
        this.httpClient = httpClient;
        this.wenXinConfig = wenXinConfig;
    }

    public Response<AiMessage> generate(List<ChatMessage> messages) {
        JSONObject payload = new JSONObject();
        payload.put("model", wenXinConfig.MODEL);
        payload.put("messages", toDeepSeekMessages(messages));
        payload.put("temperature", 0.7);
        payload.put("top_p", 0.9);
        payload.put("stream", false);

        try {
            Request request = new Request.Builder()
                    .url(wenXinConfig.chatCompletionsUrl())
                    .post(RequestBody.create(JSON, payload.toJSONString()))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", wenXinConfig.authorizationHeader())
                    .build();
            okhttp3.Response resp = httpClient.newCall(request).execute();
            String raw = resp.body() == null ? "" : resp.body().string();
            if (!resp.isSuccessful()) {
                return Response.from(AiMessage.from(""));
            }
            return Response.from(AiMessage.from(extractMessageContent(raw)));
        } catch (Exception e) {
            return Response.from(AiMessage.from(""));
        }
    }

    public Response<AiMessage> generate(String userMessage) {
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", userMessage == null ? "" : userMessage);

        JSONObject payload = new JSONObject();
        JSONArray messages = new JSONArray();
        messages.add(message);

        payload.put("model", wenXinConfig.MODEL);
        payload.put("messages", messages);
        payload.put("temperature", 0.7);
        payload.put("top_p", 0.9);
        payload.put("stream", false);

        try {
            Request request = new Request.Builder()
                    .url(wenXinConfig.chatCompletionsUrl())
                    .post(RequestBody.create(JSON, payload.toJSONString()))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", wenXinConfig.authorizationHeader())
                    .build();
            okhttp3.Response resp = httpClient.newCall(request).execute();
            String raw = resp.body() == null ? "" : resp.body().string();
            if (!resp.isSuccessful()) {
                return Response.from(AiMessage.from(""));
            }
            return Response.from(AiMessage.from(extractMessageContent(raw)));
        } catch (Exception e) {
            return Response.from(AiMessage.from(""));
        }
    }

    private JSONArray toDeepSeekMessages(List<ChatMessage> messages) {
        JSONArray result = new JSONArray();
        if (messages == null || messages.isEmpty()) {
            return result;
        }
        for (ChatMessage message : messages) {
            JSONObject item = new JSONObject();
            item.put("role", roleOf(message));
            item.put("content", contentOf(message));
            result.add(item);
        }
        return result;
    }

    private String roleOf(ChatMessage message) {
        if (message instanceof SystemMessage) {
            return "system";
        }
        if (message instanceof AiMessage) {
            return "assistant";
        }
        return "user";
    }

    private String contentOf(ChatMessage message) {
        if (message instanceof SystemMessage) {
            return ((SystemMessage) message).text();
        }
        if (message instanceof UserMessage) {
            return ((UserMessage) message).singleText();
        }
        if (message instanceof AiMessage) {
            return ((AiMessage) message).text();
        }
        return String.valueOf(message);
    }

    private String extractMessageContent(String raw) {
        JSONObject root = JSONObject.parseObject(raw);
        JSONObject error = root.getJSONObject("error");
        if (error != null) {
            return "";
        }
        JSONArray choices = root.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        JSONObject choice = choices.getJSONObject(0);
        JSONObject message = choice.getJSONObject("message");
        if (message != null) {
            return message.getString("content") == null ? "" : message.getString("content");
        }
        JSONObject delta = choice.getJSONObject("delta");
        return delta == null || delta.getString("content") == null ? "" : delta.getString("content");
    }
}
