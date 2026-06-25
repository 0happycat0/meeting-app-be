package com.happycat.meetingappbe.service;

import com.happycat.meetingappbe.configuration.LlmConfig;
import com.happycat.meetingappbe.exception.AppException;
import com.happycat.meetingappbe.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LlmChatClient {
    @Qualifier("llmWebClient")
    WebClient webClient;
    LlmConfig llmConfig;
    JsonMapper jsonMapper;

    public String complete(String prompt, int maxTokens) {
        Map<String, Object> request = Map.of(
                "model", llmConfig.getModel(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt
                )),
                "stream", true,
                "temperature", 0.2,
                "max_tokens", maxTokens
        );

        String content = webClient.post()
                .uri(llmConfig.getChatCompletionsPath())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(llmConfig.getTimeoutSeconds()))
                .map(this::extractContent)
                .filter(this::hasStreamContent)
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .block();

        if (content == null || content.isBlank()) {
            throw new AppException(ErrorCode.MEETING_MINUTES_GENERATION_FAILED);
        }
        return content.trim();
    }

    String extractContent(String eventData) {
        if (eventData == null || eventData.isBlank()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (String rawLine : eventData.split("\\R")) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                continue;
            }
            if (line.startsWith("data:")) {
                line = line.substring("data:".length()).trim();
            }
            if ("[DONE]".equals(line)) {
                continue;
            }
            appendContent(line, result);
        }
        return result.toString();
    }

    boolean hasStreamContent(String chunk) {
        return chunk != null && !chunk.isEmpty();
    }

    private void appendContent(String json, StringBuilder result) {
        try {
            JsonNode root = jsonMapper.readTree(json);
            JsonNode choices = root.path("choices");
            if (!choices.isArray()) {
                return;
            }
            for (JsonNode choice : choices) {
                JsonNode content = choice.path("delta").path("content");
                if (content.isTextual()) {
                    result.append(content.asText());
                }
            }
        } catch (Exception exception) {
            log.debug("Unable to parse LLM stream chunk", exception);
            throw new AppException(ErrorCode.MEETING_MINUTES_GENERATION_FAILED);
        }
    }
}
