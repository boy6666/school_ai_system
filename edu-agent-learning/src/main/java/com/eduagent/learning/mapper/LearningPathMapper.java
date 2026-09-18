package com.eduagent.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.learning.entity.LearningPath;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LearningPathMapper extends BaseMapper<LearningPath> {

    @Select("SELECT * FROM learning_paths WHERE student_id = #{studentId} AND status = 'active' ORDER BY update_time DESC LIMIT 1")
    LearningPath findActiveByStudentId(Long studentId);

    /** 幂等落库：按 studentId 的 active 路径存在则更新，否则插入（MP BaseMapper 已有 insertOrUpdate，避免签名冲突） */
    default void upsert(LearningPath lp) {
        LearningPath existing = findActiveByStudentId(lp.getStudentId());
        if (existing != null) {
            lp.setId(existing.getId());
            updateById(lp);
        } else {
            insert(lp);
        }
    }
}
