package com.voicdai.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimulationStartResponse {
    private String sessionId;
    private String callerName;
    private String callerNumber;
    private String scenarioType;
    private String difficulty;
}
