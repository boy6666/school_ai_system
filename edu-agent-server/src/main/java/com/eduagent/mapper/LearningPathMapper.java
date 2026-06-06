package com.eduagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.entity.LearningPath;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LearningPathMapper extends BaseMapper<LearningPath> {
    @Select("SELECT * FROM learning_paths WHERE student_id = #{studentId} ORDER BY update_time DESC LIMIT 1")
    LearningPath findByStudentId(Long studentId);

    default void insertOrUpdate(LearningPath lp) {
        LearningPath existing = findByStudentId(lp.getStudentId());
        if (existing != null) {
            lp.setId(existing.getId());
            updateById(lp);
        } else {
            insert(lp);
        }
    }
}
