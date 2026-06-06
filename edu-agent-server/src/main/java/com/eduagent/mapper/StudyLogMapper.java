package com.eduagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.entity.StudyLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface StudyLogMapper extends BaseMapper<StudyLog> {
    @Select("SELECT DATE(created_at) as day, module, SUM(duration_sec) as total FROM study_logs WHERE student_id = #{studentId} AND created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) GROUP BY DATE(created_at), module ORDER BY day, module")
    List<Map<String, Object>> dailyTrend(Long studentId);

    @Select("SELECT module, SUM(duration_sec) as total FROM study_logs WHERE student_id = #{studentId} AND DATE(created_at) = CURDATE() GROUP BY module")
    List<java.util.Map<String, Object>> todaySummary(Long studentId);
    
    @Select("SELECT SUM(duration_sec) FROM study_logs WHERE student_id = #{studentId}")
    Integer totalDuration(Long studentId);
}
