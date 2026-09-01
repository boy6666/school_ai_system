package com.eduagent.teacher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateClassRequest {

    @NotBlank(message = "班级名称不能为空")
    @Size(max = 64, message = "班级名称过长")
    private String name;

    @Size(max = 64, message = "课程名过长")
    private String course;

    @Size(max = 32, message = "学期过长")
    private String semester;
}
