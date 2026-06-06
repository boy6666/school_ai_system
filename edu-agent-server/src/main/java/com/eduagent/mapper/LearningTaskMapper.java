package com.eduagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.entity.LearningTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface LearningTaskMapper extends BaseMapper<LearningTask> {
    
    @Select("SELECT * FROM learning_tasks WHERE user_id = #{userId} ORDER BY create_time")
    List<LearningTask> selectByUserId(Long userId);
    
    @Select("SELECT * FROM learning_tasks WHERE user_id = #{userId} AND status != 'done' ORDER BY create_time")
    List<LearningTask> selectPendingByUserId(Long userId);
}
