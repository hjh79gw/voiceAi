package com.voicdai.backend.service;

import com.voicdai.backend.dto.TtsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class AudioServiceImpl implements AudioService {

    private final WebClient webClient;

    @Value("${elevenlabs.voice-id}")
    private String voiceId;

    public AudioServiceImpl(@Value("${audio-service.url}") String audioServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(audioServiceUrl)
                .build();
    }

    @Override
    public byte[] textToSpeechStream(String text, String scenarioType, boolean voipEffect) {
        TtsRequest request = TtsRequest.builder()
                .text(text)
                .voiceId(voiceId)
                .scenarioType(scenarioType)
                .voipEffect(voipEffect)
                .build();

        return webClient.post()
                .uri("/api/tts/stream")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }

    @Override
    public byte[] textToSpeech(String text, String scenarioType, boolean voipEffect) {
        TtsRequest request = TtsRequest.builder()
                .text(text)
                .voiceId(voiceId)
                .scenarioType(scenarioType)
                .voipEffect(voipEffect)
                .build();

        return webClient.post()
                .uri("/api/tts")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }
}
