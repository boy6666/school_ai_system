package com.eduagent.teacher.mq;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 教师看板缓存（进程内实现）。
 * StudyProgressConsumer 消费 study.progress 后写入；AnalyticsService 聚合时读取合并，
 * 使看板近实时而避免每次全量重算。
 * 说明：生产建议换 Redis（key=teacher:class:{classId}:dashboard），本期无 Redis 依赖，
 * 先用并发 Map 承载，接口保持一致，后续可平替。
 */
@Component
public class DashboardCache {

    private final Map<String, Map<Long, StudyProgressEvent>> store = new ConcurrentHashMap<>();

    /** 写入某班级下的某生最新进度 */
    public void put(Long classId, StudyProgressEvent event) {
        store.computeIfAbsent(key(classId), k -> new ConcurrentHashMap<>())
                .put(event.getStudentId(), event);
    }

    /** 读取某班级全部缓存进度；无则空 Map */
    public Map<Long, StudyProgressEvent> get(Long classId) {
        Map<Long, StudyProgressEvent> m = store.get(key(classId));
        return m == null ? Map.of() : Map.copyOf(m);
    }

    public List<StudyProgressEvent> values(Long classId) {
        return store.getOrDefault(key(classId), Map.of()).values()
                .stream().collect(Collectors.toList());
    }

    private String key(Long classId) {
        return "teacher:class:" + classId + ":dashboard";
    }
}
