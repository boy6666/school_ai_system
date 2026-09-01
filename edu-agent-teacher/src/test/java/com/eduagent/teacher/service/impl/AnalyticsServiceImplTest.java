package com.eduagent.teacher.service.impl;

import com.eduagent.common.result.Result;
import com.eduagent.common.security.AuthContext;
import com.eduagent.teacher.entity.Classes;
import com.eduagent.teacher.feign.LearningServiceClient;
import com.eduagent.teacher.mapper.ClassesMapper;
import com.eduagent.teacher.mapper.ClassStudentMapper;
import com.eduagent.teacher.mq.DashboardCache;
import com.eduagent.teacher.vo.ClassAnalyticsVO;
import com.eduagent.teacher.vo.ClassOverviewVO;
import com.eduagent.teacher.vo.StudentProgressVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 学情看板聚合单测（§B.5）：
 * - Semaphore(8) + analyticsExecutor 并发拉取 learning 聚合
 * - 单生数据缺失可降级（null 兜底），不抛异常
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock ClassesMapper classesMapper;
    @Mock ClassStudentMapper classStudentMapper;
    @Mock LearningServiceClient learningClient;

    DashboardCache dashboardCache = new DashboardCache();
    /** 同步执行器：保证 CompletableFuture 串行、确定性 */
    Executor executor = Runnable::run;

    AnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsServiceImpl(classesMapper, classStudentMapper,
                learningClient, dashboardCache, executor);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    @DisplayName("classAnalytics：并发聚合单生 mastery/进度，得出班级均分与分布")
    void classAnalytics_aggregatesFromLearning() {
        AuthContext.set("7", "ROLE_TEACHER");
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 7L));
        when(classStudentMapper.selectStudentIds(1L)).thenReturn(List.of(2L, 3L));

        when(learningClient.getProgress(2L)).thenReturn(Result.success(
                new StudentProgressVO(2L, 80, 90, 3600, 95, List.of("链表"))));
        when(learningClient.getProgress(3L)).thenReturn(Result.success(
                new StudentProgressVO(3L, 60, 70, 1800, 80, List.of("链表", "树"))));

        ClassAnalyticsVO vo = service.classAnalytics(1L);

        assertThat(vo.studentCount()).isEqualTo(2);
        assertThat(vo.avgMastery()).isEqualTo(80.0);          // (90+70)/2
        assertThat(vo.avgPathProgress()).isEqualTo(70.0);     // (80+60)/2
        assertThat(vo.avgStudySec()).isEqualTo(2700.0);       // (3600+1800)/2
        // 弱项统计：链表2次、树1次，按次数降序
        assertThat(vo.weakTopics()).extracting(t -> t.topic())
                .containsExactly("链表", "树");
        // mastery 分布：90→level_3，70→level_2
        assertThat(vo.masteryDist().stream().filter(d -> d.level().equals("level_2")).findFirst().get().count())
                .isEqualTo(1);
        assertThat(vo.masteryDist().stream().filter(d -> d.level().equals("level_3")).findFirst().get().count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("单生学情缺失（learning 降级 null）仍返回结构，均分按有数据者计算")
    void classAnalytics_someMissingProgress_degradesGracefully() {
        AuthContext.set("7", "ROLE_TEACHER");
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 7L));
        when(classStudentMapper.selectStudentIds(1L)).thenReturn(List.of(2L, 3L));

        when(learningClient.getProgress(2L)).thenReturn(Result.success(
                new StudentProgressVO(2L, 60, 60, 1200, 70, null)));
        // 3 号：learning 抛异常 → 降级为空学情
        when(learningClient.getProgress(3L)).thenThrow(new RuntimeException("learning down"));

        ClassAnalyticsVO vo = service.classAnalytics(1L);

        assertThat(vo.studentCount()).isEqualTo(2);
        assertThat(vo.avgMastery()).isEqualTo(60.0); // 仅 2 号有数据
        assertThat(vo.weakTopics()).isEmpty();
    }

    @Test
    @DisplayName("空班级：无并发调用，返回空聚合")
    void classAnalytics_emptyClass_returnsEmpty() {
        AuthContext.set("7", "ROLE_TEACHER");
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 7L));
        when(classStudentMapper.selectStudentIds(1L)).thenReturn(List.of());

        ClassAnalyticsVO vo = service.classAnalytics(1L);

        assertThat(vo.studentCount()).isZero();
        assertThat(vo.avgMastery()).isZero();
    }

    @Test
    @DisplayName("classOverview：均分 + 完成率 + 活跃数")
    void classOverview_producesCards() {
        AuthContext.set("7", "ROLE_TEACHER");
        when(classesMapper.selectById(1L)).thenReturn(c(1L, 7L));
        when(classStudentMapper.selectStudentIds(1L)).thenReturn(List.of(2L, 3L));

        when(learningClient.getProgress(2L)).thenReturn(Result.success(
                new StudentProgressVO(2L, 100, 80, 0, 90, null)));
        when(learningClient.getProgress(3L)).thenReturn(Result.success(
                new StudentProgressVO(3L, 100, 80, 0, 90, null)));

        ClassOverviewVO vo = service.classOverview(1L);

        assertThat(vo.studentCount()).isEqualTo(2);
        assertThat(vo.avgMastery()).isEqualTo(80.0);
        assertThat(vo.completionRate()).isEqualTo(1.0);   // avgPathProgress/100
        assertThat(vo.activeStudents()).isEqualTo(2);     // 两人都有学情
    }

    private static Classes c(Long id, Long teacherId) {
        Classes c = new Classes();
        c.setId(id);
        c.setTeacherId(teacherId);
        c.setName("数据结构");
        return c;
    }
}
