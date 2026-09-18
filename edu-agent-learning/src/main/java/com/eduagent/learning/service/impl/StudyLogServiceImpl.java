package com.eduagent.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.common.result.ApiException;
import com.eduagent.common.result.ErrorCode;
import com.eduagent.learning.dto.StudyLogRequest;
import com.eduagent.learning.entity.LearningPath;
import com.eduagent.learning.entity.LearningTask;
import com.eduagent.learning.entity.StudentProfile;
import com.eduagent.learning.entity.StudyLog;
import com.eduagent.learning.mapper.LearningPathMapper;
import com.eduagent.learning.mapper.LearningTaskMapper;
import com.eduagent.learning.mapper.StudentProfileMapper;
import com.eduagent.learning.mapper.StudyLogMapper;
import com.eduagent.learning.service.ProfileService;
import com.eduagent.learning.service.StudyLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 学习日志（看板数据源）。单体的 /dashboard/summary、/tasks、/path、/report
 * 聚合能力按 spec A.2.3 收敛到 study-log 域。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyLogServiceImpl implements StudyLogService {

    private static final Set<String> ALLOWED_MODULES = Set.of("mindmap", "quiz", "reading", "code");

    private final StudyLogMapper studyLogMapper;
    private final LearningTaskMapper taskMapper;
    private final LearningPathMapper pathMapper;
    private final StudentProfileMapper profileMapper;
    private final ProfileService profileService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> addLog(Long studentId, StudyLogRequest request) {
        if (request.getModule() == null || !ALLOWED_MODULES.contains(request.getModule())) {
            throw new ApiException(ErrorCode.BAD_REQUEST.getCode(), "module 取值必须为 mindmap/quiz/reading/code");
        }
        if (request.getDurationSec() == null || request.getDurationSec() <= 0) {
            throw new ApiException(ErrorCode.BAD_REQUEST.getCode(), "durationSec 必须为正整数");
        }
        StudyLog logEntry = new StudyLog();
        logEntry.setStudentId(studentId);
        logEntry.setModule(request.getModule());
        logEntry.setDurationSec(request.getDurationSec());
        logEntry.setChapterId(request.getChapterId());
        logEntry.setNoteId(request.getNoteId());
        logEntry.setCreatedAt(LocalDateTime.now());
        studyLogMapper.insert(logEntry);
        return Map.of("id", logEntry.getId());
    }

    @Override
    public Map<String, Object> summary(Long studentId) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("today", studyLogMapper.todaySummary(studentId));
        Integer total = studyLogMapper.totalDuration(studentId);
        r.put("totalSec", total != null ? total : 0);
        return r;
    }

    @Override
    public Map<String, Object> report(Long studentId) {
        Map<String, Object> r = new LinkedHashMap<>();

        Integer total = studyLogMapper.totalDuration(studentId);
        r.put("totalSec", total != null ? total : 0);
        r.put("modules", studyLogMapper.moduleSummary(studentId));
        r.put("trend", studyLogMapper.dailyTrend(studentId));

        StudentProfile sp = profileMapper.findByStudentId(studentId);
        if (sp != null) {
            r.put("score", sp.getLastScore() != null ? sp.getLastScore() : 0);
            r.put("learning_goal", sp.getLearningGoal());
            r.put("cognitive_style", sp.getCognitiveStyle());
            r.put("pace", sp.getPace());
            r.put("topic", sp.getTopic());
            r.put("course", sp.getCourse());
            r.put("last_suggestion", sp.getLastSuggestion());
            r.put("profile_suggestions", splitLines(sp.getProfileSuggestions()));
            r.put("weaknesses", profileService.parseJsonArray(sp.getWeaknesses()));
            if (sp.getProfileData() != null) {
                try {
                    r.put("profile_data", objectMapper.readValue(sp.getProfileData(),
                            new TypeReference<Map<String, Object>>() { }));
                } catch (Exception ignored) { }
            }
        }

        LearningPath lp = pathMapper.findActiveByStudentId(studentId);
        if (lp != null) {
            r.put("goal", lp.getGoal());
            r.put("progress", lp.getProgress());
        }
        return r;
    }

    @Override
    public List<Map<String, Object>> pendingTasks(Long studentId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (LearningTask t : taskMapper.selectPendingByUserId(studentId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("status", t.getStatus());
            m.put("priority", t.getPriority());
            out.add(m);
        }
        return out;
    }

    @Override
    public Map<String, Object> pathSummary(Long studentId) {
        Map<String, Object> r = new LinkedHashMap<>();
        LearningPath lp = pathMapper.findActiveByStudentId(studentId);
        if (lp != null) {
            r.put("goal", lp.getGoal());
            r.put("pace", lp.getPace());
            r.put("progress", lp.getProgress());
            r.put("suggestions", lp.getSuggestions());
        }
        return r;
    }

    private List<String> splitLines(String text) {
        if (text == null) return null;
        List<String> out = new ArrayList<>();
        for (String s : text.split("\\n")) {
            if (!s.isBlank()) out.add(s);
        }
        return out;
    }
}
