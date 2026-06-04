package org.musi.AI4Education.service;

import java.io.IOException;
import java.util.List;

public interface PromptTemplateService {
    String readText(String path) throws IOException;

    List<String> readLines(String path) throws IOException;
}
