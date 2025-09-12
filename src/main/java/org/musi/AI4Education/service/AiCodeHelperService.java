package org.musi.AI4Education.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

import java.util.List;
public interface AiCodeHelperService {

    @SystemMessage(fromResource = "system_prompt.txt")
    String chat(String userMessage);

    @SystemMessage(fromResource = "system_prompt.txt")
    Report chatForReport(String userMessage);

    record Report(String name, List<String> suggestionList){};

    @SystemMessage(fromResource = "system_prompt.txt")
    Result<String> chatWithRag(String userMessage);

    Flux<String> chatStream(@UserMessage String message);

}
