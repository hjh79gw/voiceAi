package com.voicdai.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TtsRequest {
    private String text;

    @JsonProperty("voice_id")
    private String voiceId;

    @JsonProperty("scenario_type")
    private String scenarioType;

    @JsonProperty("voip_effect")
    private boolean voipEffect;
}
