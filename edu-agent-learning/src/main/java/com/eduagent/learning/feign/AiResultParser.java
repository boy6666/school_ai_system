package com.eduagent.learning.feign;

import com.eduagent.learning.vo.AiChatResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 返回宽容解析器。目标契约是 common.Result 包装（code=0 + data），
 * 但联调期 AI 可能返回裸 JSON（旧版无包装）——两种形态统一剥离出 data：
 *  - {"code":0,"message":"...","data":{...}} → data 节点
 *  - {...} 裸对象 → 整个节点
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiResultParser {

    private final ObjectMapper objectMapper;

    /** 解析 AI 原始响应为 data 节点；不可解析返回 null（由调用方降级） */
    public JsonNode parseData(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(stripFences(raw));
            if (root.isObject() && root.has("code") && root.has("data")) {
                JsonNode data = root.get("data");
                return data == null || data.isNull() || data.isMissingNode() ? null : data;
            }
            return root;
        } catch (Exception e) {
            log.warn("[AI] 响应解析失败: {}", e.getMessage());
            return null;
        }
    }

    public <T> T toObject(JsonNode node, Class<T> type) {
        if (node == null) {
            return null;
        }
        try {
            return objectMapper.treeToValue(node, type);
        } catch (Exception e) {
            log.warn("[AI] data 转 {} 失败: {}", type.getSimpleName(), e.getMessage());
            return null;
        }
    }

    public AiChatResult parseChatResult(String raw) {
        return toObject(parseData(raw), AiChatResult.class);
    }

    /** data.suggestions 数组（mode=suggestion），容忍直接顶层的 suggestions 字段 */
    public List<String> parseSuggestions(String raw) {
        JsonNode data = parseData(raw);
        if (data == null) {
            return List.of();
        }
        JsonNode arr = data.has("suggestions") ? data.get("suggestions") : null;
        List<String> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            arr.forEach(n -> out.add(n.asText()));
        } else if (arr != null && arr.isTextual()) {
            out.add(arr.asText());
        }
        return out;
    }

    /** mode=judge → {score(0|1), correct, comment, explanation?} */
    public JudgeOutcome parseJudge(String raw) {
        JsonNode data = parseData(raw);
        if (data == null) {
            return null;
        }
        JudgeOutcome o = new JudgeOutcome();
        if (data.has("correct")) {
            o.correct = data.get("correct").asBoolean();
        } else if (data.has("score")) {
            o.correct = data.get("score").asInt() > 0;
        } else {
            return null;
        }
        o.score = o.correct ? 1 : 0;
        o.comment = data.has("comment") && !data.get("comment").isNull() ? data.get("comment").asText() : null;
        o.explanation = data.has("explanation") && !data.get("explanation").isNull() ? data.get("explanation").asText() : null;
        return o;
    }

    /** /path/generate 的 data → LearningPathVO 形状（容忍 suggestions 为数组或字符串） */
    public Map<String, Object> parsePathData(String raw) {
        JsonNode data = parseData(raw);
        if (data == null) {
            return null;
        }
        try {
            JsonNode sug = data.get("suggestions");
            if (sug != null && sug.isArray()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) data)
                        .put("suggestions", joinArray(sug));
            }
            JsonNode lp = data.get("learningPath");
            if (lp != null && lp.isObject()) {
                return objectMapper.convertValue(lp, new TypeReference<Map<String, Object>>() { });
            }
            return objectMapper.convertValue(data, new TypeReference<Map<String, Object>>() { });
        } catch (Exception e) {
            log.warn("[AI] 路径 data 转换失败: {}", e.getMessage());
            return null;
        }
    }

    /** 从 LLM 文本中提取 JSON 对象（容忍 ```json 围栏与前后说明文字） */
    public Map<String, Object> parseEmbeddedJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = stripFences(text).trim();
        try {
            return objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() { });
        } catch (Exception ignored) {
            // 找第一个 { 与最后一个 } 再试
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                try {
                    return objectMapper.readValue(cleaned.substring(start, end + 1), new TypeReference<Map<String, Object>>() { });
                } catch (Exception e) {
                    log.warn("[AI] 内嵌 JSON 解析失败: {}", e.getMessage());
                }
            }
        }
        return null;
    }

    private String joinArray(JsonNode arr) {
        List<String> items = new ArrayList<>();
        arr.forEach(n -> items.add(n.asText()));
        return String.join("；", items);
    }

    private String stripFences(String text) {
        return text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
    }

    @lombok.Data
    public static class JudgeOutcome {
        private boolean correct;
        private int score;
        private String comment;
        private String explanation;
    }
}
