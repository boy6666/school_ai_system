package com.eduagent.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("student_profile")
public class StudentProfile {
    @TableId
    private Long id;
    private Long studentId;
    private String goal;
    private String weaknesses;
    private String strengths;
    private String style;
}
