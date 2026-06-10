package com.eduagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfile> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM student_profiles WHERE student_id = #{studentId}")
    StudentProfile findByStudentId(Long studentId);
    default void insertOrUpdate(StudentProfile sp) {
        if (sp.getStudentId() != null && selectById(sp.getStudentId()) != null) {
            updateById(sp);
        } else {
            insert(sp);
        }
    }
}
