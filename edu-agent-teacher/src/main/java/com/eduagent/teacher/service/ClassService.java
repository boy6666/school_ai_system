package com.eduagent.teacher.service;

import com.eduagent.teacher.dto.CreateClassRequest;
import com.eduagent.teacher.dto.UpdateClassRequest;
import com.eduagent.teacher.vo.ClassStudentVO;
import com.eduagent.teacher.vo.ClassVO;

import java.util.List;

public interface ClassService {

    ClassVO create(CreateClassRequest request);

    /** 当前教师（AuthContext）本人的全部班级 */
    List<ClassVO> list();

    /** 详情 + 学生数；校验属主 */
    ClassVO get(Long id);

    ClassVO update(Long id, UpdateClassRequest request);

    void delete(Long id);

    void addStudent(Long classId, Long studentId);

    void removeStudent(Long classId, Long studentId);

    List<ClassStudentVO> listStudents(Long classId);
}
