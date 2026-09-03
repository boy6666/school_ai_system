package com.eduagent.resource.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AiPathReq {
    private String studentId;
    private String prompt;
    private Map<String, Object> profile;
}
