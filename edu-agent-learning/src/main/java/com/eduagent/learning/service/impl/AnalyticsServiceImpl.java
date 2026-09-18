package com.eduagent.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eduagent.learning.entity.LearningPath;
import com.eduagent.learning.entity.LearningTask;
import com.eduagent.learning.entity.QuizAnswer;
import com.eduagent.learning.entity.Report;
import com.eduagent.learning.entity.StudentProfile;
import com.eduagent.learning.mapper.LearningPathMapper;
import com.eduagent.learning.mapper.LearningTaskMapper;
import com.eduagent.learning.mapper.QuizAnswerMapper;
import com.eduagent.learning.mapper.ReportMapper;
import com.eduagent.learning.mapper.StudyLogMapper;
import com.eduagent.learning.mapper.StudentProfileMapper;
import com.eduagent.learning.service.AnalyticsService;
import com.eduagent.learning.vo.StudentAnalyticsVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学情统计聚合（教师看板）。仅读本服务数据：画像 / 路径 / 日志 / 测验 / 报告，
 * 不跨服务取数（班级归属等由 teacher 侧补充）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final StudentProfileMapper profileMapper;
    private final LearningPathMapper pathMapper;
    private final LearningTaskMapper taskMapper;
    private final StudyLogMapper studyLogMapper;
    private final QuizAnswerMapper quizAnswerMapper;
    private final ReportMapper reportMapper;
    private final ObjectMapper objectMapper;

    @Override
    public StudentAnalyticsVO studentAnalytics(Long studentId) {
        StudentAnalyticsVO vo = new StudentAnalyticsVO();
        vo.setStudentId(studentId);

        StudentProfile sp = profileMapper.findByStudentId(studentId);
        if (sp != null) {
            vo.setClassId(sp.getClassId());
            vo.setCourse(sp.getCourse());
            vo.setTopic(sp.getTopic());
            vo.setLastScore(sp.getLastScore());
            vo.setProfileComplete(sp.getProfileComplete() != null && sp.getProfileComplete() == 1);
            if (sp.getProfileData() != null) {
                try {
                    vo.setDimensions(objectMapper.readValue(sp.getProfileData(),
                            new TypeReference<Map<String, Object>>() { }));
                } catch (Exception ignored) { }
            }
        }

        // path
        StudentAnalyticsVO.PathSummary path = new StudentAnalyticsVO.PathSummary();
        List<LearningTask> tasks = taskMapper.selectByUserId(studentId);
        long done = tasks.stream().filter(t -> "done".equals(t.getStatus())).count();
        path.setCompletedTasks((int) done);
        path.setTotalTasks(tasks.size());
        path.setProgress(tasks.isEmpty() ? 0 : (int) Math.round(done * 100.0 / tasks.size()));
        LearningPath lp = pathMapper.findActiveByStudentId(studentId);
        path.setGoal(lp != null ? lp.getGoal() : null);
        if (lp != null && lp.getProgress() != null) {
            path.setProgress(lp.getProgress());
        }
        vo.setPath(path);

        // study
        StudentAnalyticsVO.StudySummary study = new StudentAnalyticsVO.StudySummary();
        Integer totalSec = studyLogMapper.totalDuration(studentId);
        study.setTotalSec(totalSec != null ? totalSec : 0);
        study.setModules(studyLogMapper.moduleSummary(studentId));
        vo.setStudy(study);

        // quiz
        Long answered = quizAnswerMapper.selectCount(
                new LambdaQueryWrapper<QuizAnswer>().eq(QuizAnswer::getStudentId, studentId));
        Long wrong = quizAnswerMapper.selectCount(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getStudentId, studentId).eq(QuizAnswer::getIsCorrect, 0));
        StudentAnalyticsVO.QuizSummary quiz = new StudentAnalyticsVO.QuizSummary();
        quiz.setAnswered(answered);
        quiz.setWrong(wrong);
        quiz.setAccuracy(answered > 0 ? Math.round((answered - wrong) * 100.0 / answered) / 100.0 : 0.0);
        vo.setQuiz(quiz);

        // recentReports
        vo.setRecentReports(reportMapper.selectList(new LambdaQueryWrapper<Report>()
                        .eq(Report::getStudentId, studentId)
                        .orderByDesc(Report::getCreateTime)
                        .last("LIMIT 5"))
                .stream().map(r -> {
                    StudentAnalyticsVO.RecentReport rr = new StudentAnalyticsVO.RecentReport();
                    rr.setId(r.getId());
                    rr.setTitle(r.getTitle());
                    rr.setCreateTime(r.getCreateTime());
                    return rr;
                }).toList());
        return vo;
    }

    @Override
    public Map<String, Object> studentProgress(Long studentId) {
        Map<String, Object> out = new LinkedHashMap<>();

        List<LearningTask> tasks = taskMapper.selectByUserId(studentId);
        int total = tasks.size();
        int done = (int) tasks.stream().filter(t -> "done".equals(t.getStatus())).count();
        out.put("completedTasks", done);
        out.put("totalTasks", total);
        out.put("progress", total > 0 ? (int) Math.round(done * 100.0 / total) : 0);

        StudentProfile sp = profileMapper.findByStudentId(studentId);
        out.put("lastScore", sp != null ? sp.getLastScore() : null);
        out.put("masteryRate", overallScore(sp));

        Integer totalSec = studyLogMapper.totalDuration(studentId);
        out.put("totalStudySec", totalSec != null ? totalSec : 0);
        out.put("wrongCount", quizAnswerMapper.selectCount(new LambdaQueryWrapper<QuizAnswer>()
                .eq(QuizAnswer::getStudentId, studentId).eq(QuizAnswer::getIsCorrect, 0)));

        LearningPath lp = pathMapper.findActiveByStudentId(studentId);
        out.put("pathGoal", lp != null ? lp.getGoal() : null);
        return out;
    }

    private Integer overallScore(StudentProfile sp) {
        if (sp == null || sp.getProfileData() == null) return null;
        try {
            Map<String, Object> dims = objectMapper.readValue(sp.getProfileData(),
                    new TypeReference<Map<String, Object>>() { });
            if (dims.get("overall_level") instanceof Map<?, ?> overall
                    && overall.get("score") instanceof Number n) {
                return n.intValue();
            }
        } catch (Exception ignored) { }
        return null;
    }
}
