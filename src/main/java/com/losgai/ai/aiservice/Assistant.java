package com.losgai.ai.aiservice;

import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface Assistant {

//    @SystemMessage("You are a helpful assistant.")
//    String chat(@UserMessage String userMessage);

    @SystemMessage("You are a polite assistant")
    Flux<String> chat(String userMessage);

}
