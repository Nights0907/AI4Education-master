package org.musi.AI4Education.service.impl;

import org.musi.AI4Education.service.PromptTemplateService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {
    @Override
    public String readText(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    @Override
    public List<String> readLines(String path) throws IOException {
        return Files.readAllLines(Path.of(path), StandardCharsets.UTF_8);
    }
}
