package com.eduagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT * FROM conversation WHERE student_id = #{studentId} ORDER BY create_time DESC")
    List<Conversation> selectByStudentId(Long studentId);

    @Select("SELECT * FROM conversation WHERE student_id = #{studentId} AND session_id = #{sessionId} ORDER BY create_time DESC")
    List<Conversation> selectBySession(Long studentId, String sessionId);
}
