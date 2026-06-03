package org.musi.AI4Education.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Wen_XinConfig {

    @Value("${deepseek.api-key}")
    public String API_KEY;

    @Value("${deepseek.base-url}")
    public String BASE_URL;

    @Value("${deepseek.chat-completions-path}")
    public String CHAT_COMPLETIONS_PATH;

    @Value("${deepseek.model}")
    public String MODEL;

    public String chatCompletionsUrl() {
        return BASE_URL + CHAT_COMPLETIONS_PATH;
    }

    public String authorizationHeader() {
        return "Bearer " + API_KEY;
    }
}
