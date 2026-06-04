package org.musi.AI4Education.service.impl;

import org.musi.AI4Education.service.PromptTemplateService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {
    private static final String PROMPT_ROOT = "prompts/";
    private static final String PROMPT_SUFFIX = ".txt";

    private final ConcurrentMap<String, String> promptCache = new ConcurrentHashMap<>();

    @Override
    public String getPrompt(String key) {
        return promptCache.computeIfAbsent(key, this::loadPrompt);
    }

    @Override
    public List<String> getPromptLines(String key) {
        return Arrays.asList(getPrompt(key).split("\\R"));
    }

    @Override
    public String renderPrompt(String key, Map<String, Object> variables) {
        String prompt = getPrompt(key);
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            prompt = prompt.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return prompt;
    }

    @Override
    public String readText(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    @Override
    public List<String> readLines(String path) throws IOException {
        return Files.readAllLines(Path.of(path), StandardCharsets.UTF_8);
    }

    private String loadPrompt(String key) {
        ClassPathResource resource = new ClassPathResource(PROMPT_ROOT + key + PROMPT_SUFFIX);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Prompt not found: " + key);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load prompt: " + key, e);
        }
    }
}
