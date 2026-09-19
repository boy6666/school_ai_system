package com.eduagent.learning.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * edu-agent-ai /chat 响应 data。按新契约（§1.3.1/C4）字段 camelCase；
 * @JsonAlias 兼容旧单体 snake_case 返回，降低联调期耦合。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatResult {

    private String intent;

    @JsonAlias({"answer", "final_answer"})
    private String finalAnswer;

    /** 统一 Java 契约中的附加结果；Learning 会从中提取画像等扩展字段。 */
    private Map<String, Object> references;

    @JsonAlias("intent_confidence")
    private Object intentConfidence;

    @JsonAlias("route_reason")
    private String routeReason;

    /** AI 回传的画像增量（六维 + 辅助字段），由 learning 唯一落库 */
    private Map<String, Object> profile;

    private Object resources;

    private Object learningPath;

    @JsonAlias("safety_report")
    private Map<String, Object> safetyReport;

    @JsonAlias("evaluation_report")
    private Map<String, Object> evaluationReport;

    @JsonAlias("resource_dir")
    private String resourceDir;

    @JsonAlias("profile_complete")
    private Boolean profileComplete;
}
