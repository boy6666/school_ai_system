package com.eduagent.teacher.mq;

import com.eduagent.common.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学习进度事件（来自 edu-agent-learning，exchange=routingKey=study.progress）。
 * 本服务消费后更新看板缓存，使教师看板近实时。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StudyProgressEvent extends BaseEvent {

    public static final String TOPIC = "study.progress";

    private Long classId;
    private Long studentId;
    private Integer pathProgress;
    private Integer knowledgeMastery;
    private Integer learningSeconds;
    private String lastTopic;
}
