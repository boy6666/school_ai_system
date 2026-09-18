package com.eduagent.learning.service.impl;

import com.eduagent.learning.entity.LearningPath;
import com.eduagent.learning.entity.LearningPathHistory;
import com.eduagent.learning.entity.LearningTask;
import com.eduagent.learning.feign.AiResultParser;
import com.eduagent.learning.feign.AiServiceClient;
import com.eduagent.learning.mapper.LearningPathHistoryMapper;
import com.eduagent.learning.mapper.LearningPathMapper;
import com.eduagent.learning.mapper.LearningTaskMapper;
import com.eduagent.learning.mapper.StudentProfileMapper;
import com.eduagent.learning.mq.StudyProgressPublisher;
import com.eduagent.learning.vo.LearningPathVO;
import com.eduagent.learning.vo.StageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 路径域：AI 成功链路（补全聚合字段 + 任务/历史落库）与 AI 失败 fallback。 */
@ExtendWith(MockitoExtension.class)
class LearningPathServiceImplTest {

    @Mock
    private LearningPathMapper pathMapper;
    @Mock
    private LearningTaskMapper taskMapper;
    @Mock
    private LearningPathHistoryMapper historyMapper;
    @Mock
    private StudentProfileMapper profileMapper;
    @Mock
    private AiServiceClient aiServiceClient;
    @Mock
    private StudyProgressPublisher progressPublisher;

    private LearningPathServiceImpl service;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new LearningPathServiceImpl(pathMapper, taskMapper, historyMapper,
                profileMapper, aiServiceClient, new AiResultParser(om), progressPublisher, om);
        // 模拟 DB 自增主键回填（部分用例不触发 insert，用 lenient 规避严格模式）
        org.mockito.Mockito.lenient().doAnswer(inv -> {
            LearningTask t = inv.getArgument(0);
            t.setId(System.nanoTime());
            return 1;
        }).when(taskMapper).insert(any(LearningTask.class));
    }

    @Test
    void generatePath_aiSuccess_fillsAggregatesAndPersists() {
        when(profileMapper.findByStudentId(1001L)).thenReturn(null);
        String aiRaw = "{\"code\":0,\"data\":{"
                + "\"goal\":\"掌握集合框架\",\"targetMastery\":\"≥85%\",\"totalHours\":24,\"masteryRate\":60,"
                + "\"stages\":["
                + "  {\"name\":\"今日计划\",\"tasks\":[{\"title\":\"复习 List\",\"duration\":30},{\"title\":\"做 5 题\",\"duration\":45}]},"
                + "  {\"name\":\"本周路径\",\"tasks\":[{\"title\":\"Map 实验\",\"duration\":60}]}"
                + "],"
                + "\"suggestions\":[\"每天 30 分钟\"],\"applicationAdvice\":\"a\",\"examAdvice\":\"e\",\"recommendTime\":\"19:00\"}}";
        when(aiServiceClient.generatePath(any())).thenReturn(aiRaw);

        LearningPathVO vo = service.generatePath(1001L);

        assertEquals("掌握集合框架", vo.getGoal());
        // C9：totalTasks/completedTasks 由 learning 自算
        assertEquals(3, vo.getTotalTasks());
        assertEquals(0, vo.getCompletedTasks());
        assertEquals(60, vo.getMasteryRate());
        assertEquals(0, vo.getLearningRate());
        assertEquals(40, vo.getUnmasteredRate());
        // 数组 suggestions 归一化为字符串
        assertEquals("每天 30 分钟", vo.getSuggestions());

        verify(taskMapper, times(3)).insert(any(LearningTask.class));
        // tasks[].id 落库回填
        assertTrue(vo.getStages().stream().flatMap(s -> s.getTasks().stream()).allMatch(t -> t.getId() != null));
        verify(pathMapper).upsert(any(LearningPath.class));
        verify(historyMapper).insert(any(LearningPathHistory.class));
    }

    @Test
    void generatePath_aiDown_fallsBackToNonEmptyPath() {
        when(profileMapper.findByStudentId(1001L)).thenReturn(null);
        when(aiServiceClient.generatePath(any())).thenThrow(new RuntimeException("503"));

        LearningPathVO vo = service.generatePath(1001L);

        assertNotNull(vo.getGoal());
        assertEquals(4, vo.getStages().size());
        assertEquals(8, vo.getTotalTasks());
        verify(taskMapper, times(8)).insert(any(LearningTask.class));
        verify(historyMapper).insert(any(LearningPathHistory.class));
    }

    @Test
    void updateTaskStatus_recalculatesProgressAndPublishesEvent() {
        LearningTask t1 = task(1L, "复习 List", "done");
        LearningTask t2 = task(2L, "做 5 题", "todo");
        LearningTask t3 = task(3L, "Map 实验", "todo");
        when(taskMapper.selectByUserId(1001L)).thenReturn(List.of(t1, t2, t3), List.of(t1, t2, t3));
        LearningPath lp = new LearningPath();
        lp.setId(10L);
        lp.setProgress(0);
        when(pathMapper.findActiveByStudentId(1001L)).thenReturn(lp);

        service.updateTaskStatus(1001L, "今日计划", "做 5 题", true);

        // t2 状态已更新为 done
        ArgumentCaptor<LearningTask> taskCaptor = ArgumentCaptor.forClass(LearningTask.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        assertEquals("done", taskCaptor.getValue().getStatus());

        // 进度重算：t1、t2 完成，t3 未完成 → 2/3 ≈ 67%，事件发布同值
        ArgumentCaptor<LearningPath> pathCaptor = ArgumentCaptor.forClass(LearningPath.class);
        verify(pathMapper).updateById(pathCaptor.capture());
        assertEquals(67, pathCaptor.getValue().getProgress());
        verify(progressPublisher).publish(1001L, 67, 2, 3);
    }

    @Test
    void updateTaskStatus_noTasks_skipsEvent() {
        when(taskMapper.selectByUserId(1001L)).thenReturn(List.of());
        when(pathMapper.findActiveByStudentId(1001L)).thenReturn(null);
        when(aiServiceClient.generatePath(any())).thenThrow(new RuntimeException("down"));
        when(profileMapper.findByStudentId(1001L)).thenReturn(null);

        service.updateTaskStatus(1001L, null, "任意", true);

        verify(progressPublisher, never()).publish(anyLong(), any(), any(), any());
    }

    private LearningTask task(long id, String title, String status) {
        LearningTask t = new LearningTask();
        t.setId(id);
        t.setUserId(1001L);
        t.setTitle(title);
        t.setStage("today");
        t.setStatus(status);
        t.setProgress("done".equals(status) ? 100 : 0);
        return t;
    }
}
