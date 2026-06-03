package org.musi.AI4Education.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
public class WenxinConfig {
    static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS).build();

    @Value("${baidu.wenxin.client-id:}")
    private String clientId;

    @Value("${baidu.wenxin.client-secret:}")
    private String clientSecret;

    public String getWenxinToken() throws IOException {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("Baidu Wenxin credentials are not configured");
        }
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "");
        String tokenUrl = UriComponentsBuilder.fromHttpUrl("https://aip.baidubce.com/oauth/2.0/token")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("grant_type", "client_credentials")
                .toUriString();
        Request request = new Request.Builder()
                .url(tokenUrl)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        String s = response.body().string();
        JSONObject objects = JSONArray.parseObject(s);
        return objects.getString("access_token");
    }
}
