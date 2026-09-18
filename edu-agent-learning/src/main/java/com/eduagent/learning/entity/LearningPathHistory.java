package com.eduagent.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 学习路径历史。 */
@Data
@TableName("learning_path_history")
public class LearningPathHistory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String goal;
    private String pathData;
    private LocalDateTime createdAt;
}
