package org.musi.AI4Education.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SseStreamParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isDone(String data) {
        if (data == null) {
            return false;
        }
        String trimmed = data.trim();
        return "[DONE]".equals(trimmed) || "data: [DONE]".equals(trimmed);
    }

    public List<String> extractContents(String data) throws IOException {
        List<String> contents = new ArrayList<>();
        if (data == null || data.isBlank()) {
            return contents;
        }
        String[] lines = data.split("\\r?\\n");
        for (String line : lines) {
            String payload = line.trim();
            if (payload.isEmpty()) {
                continue;
            }
            if (payload.startsWith("data:")) {
                payload = payload.substring(5).trim();
            }
            if (payload.isEmpty() || "[DONE]".equals(payload)) {
                continue;
            }
            JsonNode root = objectMapper.readTree(payload);
            JsonNode content = root.path("choices").path(0).path("delta").path("content");
            if (!content.isMissingNode() && !content.isNull() && !content.asText().isEmpty()) {
                contents.add(content.asText());
            }
        }
        return contents;
    }
}
