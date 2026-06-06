package com.eduagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.entity.LearningPathHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface LearningPathHistoryMapper extends BaseMapper<LearningPathHistory> {
    @Select("SELECT * FROM learning_path_history WHERE student_id = #{studentId} ORDER BY created_at DESC LIMIT 10")
    List<LearningPathHistory> findByStudentId(Long studentId);
}
