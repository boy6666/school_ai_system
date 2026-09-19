package com.eduagent.learning.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * edu-agent-ai /resource/generate 的统一 Java Feign 请求。
 * 与 teacher 服务共用 {mode, chapter, topic, type, difficulty, count, extra} 形态。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResourceRequest {

    /** quiz / evaluation / resource / judge / suggestion。 */
    private String mode;

    private String chapter;

    private String topic;

    /** 具体资源或题型；judge/suggestion 等模式也用它选择 AI 角色。 */
    private String type;

    private String difficulty;

    private Integer count;

    /** prompt、studentId、profile 等扩展信息。 */
    private Map<String, Object> extra;
}
