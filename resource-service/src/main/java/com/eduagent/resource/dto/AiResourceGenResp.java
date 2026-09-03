package com.eduagent.resource.dto;

import lombok.Data;

@Data
public class AiResourceGenResp {
    private String content;
    private String status;
    private String errorMsg;
}
