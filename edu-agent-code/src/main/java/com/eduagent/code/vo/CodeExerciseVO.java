package com.eduagent.code.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeExerciseVO {

    private Long id;

    private String title;

    private String difficulty;

    private String language;

    private Integer status;

    private LocalDateTime createTime;
}
