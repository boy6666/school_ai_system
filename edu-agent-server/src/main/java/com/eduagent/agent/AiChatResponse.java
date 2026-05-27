package com.eduagent.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AiChatResponse {
    private String intent;
    @JsonProperty("final_answer")
    private String finalAnswer;
    private Object profile;
    private Object resources;
    @JsonProperty("learning_path")
    private Object learningPath;
    @JsonProperty("safety_report")
    private Object safetyReport;
    @JsonProperty("evaluation_report")
    private Object evaluationReport;
    @JsonProperty("resource_dir")
    private String resourceDir;
}
