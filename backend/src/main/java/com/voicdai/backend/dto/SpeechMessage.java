package com.voicdai.backend.dto;

import lombok.Data;

@Data
public class SpeechMessage {
    private String sessionId;
    private String text;
}
