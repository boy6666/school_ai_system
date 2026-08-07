package com.eduagent.teacher.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * AI 内容生成请求（Feign→ai /resource/generate）。
 * mode 必填，按 mode 解析返回：quiz→items / evaluation→analysis / resource→content 等。
 */
@Data
@Builder
public class AiResourceRequest {

    /** quiz / evaluation / resource */
    private String mode;

    private String chapter;
    private String topic;
    private String type;
    private String difficulty;
    private Integer count;

    /** 扩展参数（如出题要求、评价对象等） */
    private Map<String, Object> extra;
}
