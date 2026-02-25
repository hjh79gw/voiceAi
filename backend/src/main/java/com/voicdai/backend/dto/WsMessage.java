package com.voicdai.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WsMessage {
    private String type;     // ai_text, ai_audio, turn_end, call_ended
    private String text;
    private String audio;    // base64 encoded audio chunk
    private int turn;
}
