package com.eduagent.common.event;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * MQ 事件信封（基类）。各服务定义的具体事件继承此类。
 * 事件名约定（见《契约对齐决议》C12）：exchange 名 = 事件名。
 */
@Data
public abstract class BaseEvent implements Serializable {

    private String eventId;
    private String source;
    private long timestamp;

    protected BaseEvent() {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.timestamp = Instant.now().toEpochMilli();
    }
}
