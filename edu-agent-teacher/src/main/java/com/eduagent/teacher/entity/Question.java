package com.eduagent.teacher.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.eduagent.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题库实体。options 为 MySQL JSON 列，Java 侧用 String 承载（写时存 JSON 字符串，
 * 读时由 VO 解析）。creatorId 逻辑引用 auth_db.users.id。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("questions")
public class Question extends BaseEntity {

    /** choice / code / blank */
    private String type;

    private String chapter;

    private String topic;

    private String content;

    /** 选择题选项，JSON 数组字符串 */
    private String options;

    /** 参考答案 */
    private String answer;

    private String explanation;

    /** easy / medium / hard */
    private String difficulty;

    private Long creatorId;
}
