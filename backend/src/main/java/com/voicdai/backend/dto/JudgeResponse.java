package com.voicdai.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JudgeResponse {
    private boolean correct;
    private int score;
    private String feedback;
}
