package com.eduagent.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.learning.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfile> {

    @Select("SELECT * FROM student_profiles WHERE student_id = #{studentId} LIMIT 1")
    StudentProfile findByStudentId(Long studentId);

    /** 幂等落库：按 studentId 存在则更新，否则插入（MP BaseMapper 已有 insertOrUpdate，避免签名冲突） */
    default void upsert(StudentProfile sp) {
        if (sp.getId() != null) {
            updateById(sp);
        } else {
            StudentProfile existing = findByStudentId(sp.getStudentId());
            if (existing != null) {
                sp.setId(existing.getId());
                updateById(sp);
            } else {
                insert(sp);
            }
        }
    }
}
