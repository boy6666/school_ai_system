package com.eduagent.teacher.service;

import com.eduagent.teacher.dto.SubmitAssignmentRequest;
import com.eduagent.teacher.dto.UpdateGradeRequest;
import com.eduagent.teacher.vo.GradeDetailVO;
import com.eduagent.teacher.vo.GradeVO;
import com.eduagent.teacher.vo.StudentAssignmentVO;

import java.util.List;

public interface GradeService {

    /** 学生提交作业：choice/blank 本地判分；code 异步两段式（提交受理后由事件回填） */
    List<GradeVO> submit(Long assignmentId, SubmitAssignmentRequest request);

    List<GradeVO> listGrades(Long assignmentId, Long studentId);

    GradeDetailVO getGrade(Long gradeId);

    GradeVO updateGrade(Long gradeId, UpdateGradeRequest request);

    /** 某生（本人或教师）所有作业：含我的成绩 */
    List<StudentAssignmentVO> studentAssignments(Long studentId);
}
