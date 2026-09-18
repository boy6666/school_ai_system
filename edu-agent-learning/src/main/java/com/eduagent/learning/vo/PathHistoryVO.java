package com.eduagent.learning.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/** 学习路径历史项。 */
@Data
public class PathHistoryVO {

    private Long id;
    private String goal;
    private Map<String, Object> pathData;
    private LocalDateTime createTime;
}
