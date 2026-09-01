package com.eduagent.teacher.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 班级部分更新请求：仅覆盖非空字段（name/course/semester 均可选）。
 */
@Data
public class UpdateClassRequest {

    @Size(max = 64, message = "班级名称过长")
    private String name;

    @Size(max = 64, message = "课程名过长")
    private String course;

    @Size(max = 32, message = "学期过长")
    private String semester;
}
