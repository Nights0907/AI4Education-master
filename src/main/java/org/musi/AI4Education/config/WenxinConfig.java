package org.musi.AI4Education.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
public class WenxinConfig {
    static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().connectTimeout(60, TimeUnit.SECONDS) // 设置连接超时时间为120秒
        .readTimeout(60, TimeUnit.SECONDS) // 设置读取超时时间为120秒
        .writeTimeout(60, TimeUnit.SECONDS).build();
    public String getWenxinToken() throws IOException {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/oauth/2.0/token?client_id=dpDtO8esA1Xo3y6ZZOETGbPC&client_secret=Twqq79KEGa7gzbLF3LN1rfg3GcoqUQKa&grant_type=client_credentials") //按官网要求填写你申请的key和相关秘钥
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        String s = response.body().string();
        JSONObject objects = JSONArray.parseObject(s);
        String msg = objects.getString("access_token");
        System.out.println(msg);
        return msg;
    }
}
