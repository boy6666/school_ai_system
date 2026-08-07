package com.eduagent.teacher.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题库出参。options 由服务端解析为数组（存库为 JSON 字符串）。
 * answer 供教师查看；对 code 题本期由服务层置空后再透出（见细化 D8）。
 */
public record QuestionVO(Long id, String type, String chapter, String topic,
                         String content, List<String> options, String answer,
                         String explanation, String difficulty, Long creatorId,
                         LocalDateTime createTime) {
}
