package com.eduagent.teacher.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddStudentRequest {

    @NotNull(message = "studentId 不能为空")
    private Long studentId;
}
