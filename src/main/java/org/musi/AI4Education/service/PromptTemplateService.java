package org.musi.AI4Education.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface PromptTemplateService {
    String getPrompt(String key);

    List<String> getPromptLines(String key);

    String renderPrompt(String key, Map<String, Object> variables);

    String readText(String path) throws IOException;

    List<String> readLines(String path) throws IOException;
}
