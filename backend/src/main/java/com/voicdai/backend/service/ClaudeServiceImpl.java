package com.voicdai.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class ClaudeServiceImpl implements ClaudeService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${claude.api-key}")
    private String apiKey;

    @Value("${claude.model}")
    private String model;

    @Value("${claude.max-tokens}")
    private int maxTokens;

    public ClaudeServiceImpl() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.anthropic.com")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void streamChat(String systemPrompt,
                           List<Map<String, String>> conversationHistory,
                           Consumer<String> onToken,
                           Runnable onComplete) {

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", conversationHistory,
                "stream", true
        );

        webClient.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(data -> {
                    try {
                        if (data.startsWith("data: ")) {
                            data = data.substring(6);
                        }
                        if (data.equals("[DONE]") || data.isBlank()) return;

                        JsonNode json = objectMapper.readTree(data);
                        String type = json.path("type").asText();

                        if ("content_block_delta".equals(type)) {
                            String text = json.path("delta").path("text").asText("");
                            if (!text.isEmpty()) {
                                onToken.accept(text);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("SSE 파싱 스킵: {}", e.getMessage());
                    }
                })
                .doOnComplete(onComplete::run)
                .doOnError(e -> {
                    if (e instanceof WebClientResponseException wce) {
                        log.error("Claude API 에러: {} - {}", wce.getStatusCode(), wce.getResponseBodyAsString());
                    } else {
                        log.error("Claude API 에러: {}", e.getMessage());
                    }
                })
                .subscribe();
    }

    @Override
    public String chat(String systemPrompt, List<Map<String, String>> messages) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 500,
                "system", systemPrompt,
                "messages", messages
        );

        try {
            String response = webClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json = objectMapper.readTree(response);
            return json.path("content").get(0).path("text").asText();
        } catch (Exception e) {
            log.error("Claude API 호출 실패: {}", e.getMessage());
            return "해설을 생성하지 못했습니다.";
        }
    }
}
