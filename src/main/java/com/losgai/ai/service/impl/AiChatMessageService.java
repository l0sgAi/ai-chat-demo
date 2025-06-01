package com.losgai.ai.service.impl;

import com.losgai.ai.entity.AiConfig;
import com.losgai.ai.util.OpenAiModelBuilder;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class AiChatMessageService {

    public ChatSession chatMessageStream(AiConfig config, String userMessage) {
        OpenAiStreamingChatModel model = OpenAiModelBuilder.fromAiConfigByLangChain4j(config);
        return new ChatSession(model, userMessage);
    }

    public static class ChatSession {
        private final OpenAiStreamingChatModel model;
        private final String question;

        private Consumer<String> onNextConsumer;
        private Consumer<Response<AiMessage>> onCompleteConsumer;
        private Consumer<Throwable> onErrorConsumer;

        public ChatSession(OpenAiStreamingChatModel model, String question) {
            this.model = model;
            this.question = question;
        }

        public ChatSession onNext(Consumer<String> consumer) {
            this.onNextConsumer = consumer;
            return this;
        }

        public ChatSession onComplete(Consumer<Response<AiMessage>> consumer) {
            this.onCompleteConsumer = consumer;
            return this;
        }

        public ChatSession onError(Consumer<Throwable> consumer) {
            this.onErrorConsumer = consumer;
            return this;
        }

        public void start() {
            model.generate(question, new StreamingResponseHandler<>() {
                @Override
                public void onNext(String token) {
                    if (onNextConsumer != null) {
                        onNextConsumer.accept(token);
                    }
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    if (onCompleteConsumer != null) {
                        onCompleteConsumer.accept(response);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    if (onErrorConsumer != null) {
                        onErrorConsumer.accept(throwable);
                    }
                }
            });
        }
    }
}
