package org.musi.AI4Education.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

import java.util.List;
public interface AiCodeHelperService {

    @SystemMessage(fromResource = "prompts/math/solution_steps.txt")
    String chat(String userMessage);

    @SystemMessage(fromResource = "prompts/math/solution_steps.txt")
    Report chatForReport(String userMessage);

    record Report(String name, List<String> suggestionList){};

    @SystemMessage(fromResource = "prompts/math/solution_steps.txt")
    Result<String> chatWithRag(String userMessage);

    @SystemMessage(fromResource = "prompts/math/solution_steps.txt")
    Flux<String> chatStream(@UserMessage String message);

}
