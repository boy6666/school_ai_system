package com.eduagent.learning.dto;

import lombok.Data;

import java.util.Map;

/** 引导对话请求（POST /onboard/chat）。 */
@Data
public class OnboardRequest {

    private String message;
    /** 不传默认 onboard_ + 时间戳 */
    private String sessionId;
    /** 前端已采集的画像（camelCase），可空 */
    private Map<String, Object> profile;
}
