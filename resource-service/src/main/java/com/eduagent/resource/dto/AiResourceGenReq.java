package com.eduagent.resource.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AiResourceGenReq {
    private String studentId;
    private String chapter;
    private String topic;
    private String resourceType;
    private String level;
    private String prompt;
    private Map<String, Object> profile;
}
