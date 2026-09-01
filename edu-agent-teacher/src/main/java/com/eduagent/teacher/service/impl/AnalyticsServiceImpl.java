package com.eduagent.teacher.service.impl;

import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.common.security.AuthContext;
import com.eduagent.teacher.entity.Classes;
import com.eduagent.teacher.feign.LearningServiceClient;
import com.eduagent.teacher.mapper.ClassesMapper;
import com.eduagent.teacher.mapper.ClassStudentMapper;
import com.eduagent.teacher.mq.DashboardCache;
import com.eduagent.teacher.mq.StudyProgressEvent;
import com.eduagent.teacher.service.AnalyticsService;
import com.eduagent.teacher.vo.ClassAnalyticsVO;
import com.eduagent.teacher.vo.ClassOverviewVO;
import com.eduagent.teacher.vo.StudentProgressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ClassesMapper classesMapper;
    private final ClassStudentMapper classStudentMapper;
    private final LearningServiceClient learningClient;
    private final DashboardCache dashboardCache;
    @Qualifier("analyticsExecutor")
    private final java.util.concurrent.Executor analyticsExecutor;

    /** 保护下游 learning 的并发信号量 */
    private static final int CONCURRENCY = 8;

    @Override
    public ClassAnalyticsVO classAnalytics(Long classId) {
        Classes clazz = requireOwnClass(classId);
        List<Long> studentIds = classStudentMapper.selectStudentIds(classId);
        Map<Long, StudentProgressVO> effective = aggregate(classId, studentIds);

        List<ClassAnalyticsVO.TaskEntry> taskCompletion = new ArrayList<>();
        List<ClassAnalyticsVO.WeakTopic> weakTopics = new ArrayList<>();
        Map<String, Integer> topicCount = new LinkedHashMap<>();
        int[] masteryBucket = new int[3]; // 0:level_1,1:level_2,2:level_3
        int[] hasData = {0};

        OptionalDouble avgM = effective.values().stream()
                .filter(p -> p != null && p.knowledgeMastery() != null)
                .mapToInt(StudentProgressVO::knowledgeMastery).average();
        OptionalDouble avgP = effective.values().stream()
                .filter(p -> p != null && p.pathProgress() != null)
                .mapToInt(StudentProgressVO::pathProgress).average();
        OptionalDouble avgS = effective.values().stream()
                .filter(p -> p != null && p.learningSeconds() != null)
                .mapToInt(StudentProgressVO::learningSeconds).average();

        effective.forEach((sid, p) -> {
            if (p == null) {
                return;
            }
            hasData[0]++;
            if (p.knowledgeMastery() != null) {
                int m = p.knowledgeMastery();
                masteryBucket[m < 60 ? 0 : (m < 85 ? 1 : 2)]++;
            }
            if (!CollectionUtils.isEmpty(p.weakTopics())) {
                p.weakTopics().forEach(t -> topicCount.merge(t, 1, Integer::sum));
            }
            taskCompletion.add(new ClassAnalyticsVO.TaskEntry(sid, "",
                    get(p.pathProgress()), p.lastScore()));
        });
        topicCount.forEach((t, c) -> weakTopics.add(new ClassAnalyticsVO.WeakTopic(t, c)));
        weakTopics.sort((a, b) -> b.count() - a.count());

        List<ClassAnalyticsVO.MasteryDist> masteryDist = List.of(
                new ClassAnalyticsVO.MasteryDist("level_1", masteryBucket[0]),
                new ClassAnalyticsVO.MasteryDist("level_2", masteryBucket[1]),
                new ClassAnalyticsVO.MasteryDist("level_3", masteryBucket[2]));

        Map<String, Double> dimensionAvg = Map.of("knowledge_mastery", round(avgM.orElse(0)));

        return new ClassAnalyticsVO(classId, clazz.getName(), effective.size(),
                round(avgM.orElse(0)), round(avgP.orElse(0)), round(avgS.orElse(0)),
                masteryDist, dimensionAvg, taskCompletion,
                weakTopics, List.of());
    }

    @Override
    public ClassOverviewVO classOverview(Long classId) {
        Classes clazz = requireOwnClass(classId);
        List<Long> studentIds = classStudentMapper.selectStudentIds(classId);
        Map<Long, StudentProgressVO> effective = aggregate(classId, studentIds);

        double avgM = effective.values().stream()
                .filter(p -> p != null && p.knowledgeMastery() != null)
                .mapToInt(StudentProgressVO::knowledgeMastery).average().orElse(0);
        double avgP = effective.values().stream()
                .filter(p -> p != null && p.pathProgress() != null)
                .mapToInt(StudentProgressVO::pathProgress).average().orElse(0);
        long active = effective.values().stream().filter(java.util.Objects::nonNull).count();
        return new ClassOverviewVO(classId, clazz.getName(), effective.size(),
                round(avgM), round(avgP / 100.0), (int) active);
    }

    /** 并发拉取 + study.progress 缓存合并，返回 学生id→(有效学情) */
    private Map<Long, StudentProgressVO> aggregate(Long classId, List<Long> studentIds) {
        Semaphore sem = new Semaphore(CONCURRENCY);
        Map<Long, StudentProgressVO> result = new HashMap<>();
        if (CollectionUtils.isEmpty(studentIds)) {
            return result;
        }
        List<CompletableFuture<StudentProgressVO>> futures = studentIds.stream()
                .map(sid -> CompletableFuture.supplyAsync(() -> {
                    try {
                        sem.acquire();
                        try {
                            StudentProgressVO p = learningClient.getProgress(sid).getData();
                            return p == null ? new StudentProgressVO(sid, null, null, null, null, null) : p;
                        } finally {
                            sem.release();
                        }
                    } catch (Exception e) {
                        log.warn("拉取学情失败 studentId={}: {}", sid, e.getMessage());
                        return new StudentProgressVO(sid, null, null, null, null, null);
                    }
                }, analyticsExecutor))
                .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        for (CompletableFuture<StudentProgressVO> f : futures) {
            StudentProgressVO p = f.join();
            if (p != null && p.studentId() != null) {
                result.put(p.studentId(), p);
            }
        }
        // 合并 study.progress 缓存（近实时覆盖字段）
        Map<Long, StudyProgressEvent> cached = dashboardCache.get(classId);
        if (cached != null) {
            cached.forEach((sid, ev) -> {
                if (sid == null) {
                    return;
                }
                StudentProgressVO p = result.get(sid);
                result.put(sid, new StudentProgressVO(sid,
                        ev.getPathProgress() != null ? ev.getPathProgress() : (p == null ? null : p.pathProgress()),
                        ev.getKnowledgeMastery() != null ? ev.getKnowledgeMastery() : (p == null ? null : p.knowledgeMastery()),
                        ev.getLearningSeconds() != null ? ev.getLearningSeconds() : (p == null ? null : p.learningSeconds()),
                        p == null ? null : p.lastScore(),
                        p == null ? null : p.weakTopics()));
            });
        }
        return result;
    }

    private int get(Integer v) {
        return v == null ? 0 : v;
    }

    private double round(double d) {
        return Math.round(d * 10.0) / 10.0;
    }

    private Classes requireOwnClass(Long classId) {
        Classes c = classesMapper.selectById(classId);
        if (c == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "班级不存在");
        }
        String uid = AuthContext.getUserId();
        if (uid == null || !c.getTeacherId().equals(Long.valueOf(uid))) {
            throw new ApiException(ErrorCode.FORBIDDEN, "无权查看他人班级");
        }
        return c;
    }
}
