package com.eduagent.code.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("code_exercises")
public class CodeExercise {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    /** EASY / MEDIUM / HARD */
    private String difficulty;

    private String language;

    /** 1=启用 0=禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
