package com.eduagent.teacher.vo;

/**
 * AI 答疑返回（来自 ai /chat）。
 */
public record AiChatResult(String answer, String intent, Object references) {
}
