package com.eduagent.teacher.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.teacher.entity.Grade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GradeMapper extends BaseMapper<Grade> {

    /**
     * 按 作业+学生+题目项 查唯一成绩（幂等 upsert 依据，对应 uk_stu_item）。
     * 逻辑删除已全局生效，无需手写 deleted=0。
     */
    @Select("SELECT * FROM grades WHERE assignment_id = #{assignmentId} "
            + "AND student_id = #{studentId} AND item_id = #{itemId}")
    Grade selectByStuItem(@Param("assignmentId") Long assignmentId,
                          @Param("studentId") Long studentId,
                          @Param("itemId") Long itemId);
}
