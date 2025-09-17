package org.musi.AI4Education.model;
import okhttp3.OkHttpClient;
import org.musi.AI4Education.config.Wen_XinConfig;
import org.musi.AI4Education.model.wenxin.WenxinChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Configuration
public class WenxinChatModelConfig {

    @Resource
    private Wen_XinConfig wenXinConfig;

    @Bean
    public OkHttpClient wenxinOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public WenxinChatModel wenxinChatModel(OkHttpClient wenxinOkHttpClient) {
        return new WenxinChatModel(wenxinOkHttpClient, wenXinConfig);
    }
}


