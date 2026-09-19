package com.eduagent.learning.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 教师为学生绑定班级。classId 为 teacher_db.classes.id 的逻辑引用。 */
@Data
public class BindClassRequest {

    @NotNull(message = "classId 不能为空")
    @Positive(message = "classId 必须为正整数")
    private Long classId;
}
