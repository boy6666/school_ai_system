package com.eduagent.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatResponse {

    @JsonProperty("final_answer")
    private String finalAnswer;

    private String intent;

    @JsonProperty("intent_confidence")
    private Double intentConfidence;

    @JsonProperty("route_reason")
    private String routeReason;

    private Map<String, Object> profile;

    @JsonProperty("profile_patch")
    private Map<String, Object> profilePatch;

    @JsonProperty("evaluation_report")
    private Map<String, Object> evaluationReport;

    @JsonProperty("safety_report")
    private Map<String, Object> safetyReport;

    @JsonProperty("resource_dir")
    private String resourceDir;

    @JsonProperty("profile_complete")
    private Boolean profileComplete;
}
