package com.eduagent.teacher.mapper;

import com.eduagent.teacher.entity.ClassStudent;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 班级-学生关联 Mapper。复合主键，不走 BaseMapper；用自定义 SQL。
 * deleted 列按逻辑删除习惯保留：查询只取在班（deleted=0），移除走物理 DELETE
 * （关联表无审计必要，且避免 logic-delete 的 deleted=1 记录残留占用唯一语义）。
 */
@Mapper
public interface ClassStudentMapper {

    @Insert("INSERT INTO class_students (class_id, student_id, joined_at, deleted) "
            + "VALUES (#{classId}, #{studentId}, NOW(), 0)")
    int insert(@Param("classId") Long classId, @Param("studentId") Long studentId);

    @Delete("DELETE FROM class_students WHERE class_id = #{classId} AND student_id = #{studentId}")
    int deleteRelation(@Param("classId") Long classId, @Param("studentId") Long studentId);

    @Select("SELECT student_id FROM class_students WHERE class_id = #{classId} AND deleted = 0")
    List<Long> selectStudentIds(@Param("classId") Long classId);

    @Select("SELECT class_id, student_id, joined_at FROM class_students "
            + "WHERE class_id = #{classId} AND deleted = 0 ORDER BY joined_at")
    List<ClassStudent> selectByClass(@Param("classId") Long classId);

    @Select("SELECT class_id FROM class_students WHERE student_id = #{studentId} AND deleted = 0")
    List<Long> selectClassIds(@Param("studentId") Long studentId);

    /**
     * 幂等加入：已存在（含 deleted=1 历史）时不重复插入。
     * 用 INSERT IGNORE + 唯一键感知；因无唯一约束，先查后插。
     */
    @Select("SELECT COUNT(*) FROM class_students WHERE class_id = #{classId} AND student_id = #{studentId}")
    int exists(@Param("classId") Long classId, @Param("studentId") Long studentId);
}
