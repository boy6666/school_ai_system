package com.eduagent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

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
