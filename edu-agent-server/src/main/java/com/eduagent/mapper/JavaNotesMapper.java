package com.eduagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.entity.JavaNotes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface JavaNotesMapper extends BaseMapper<JavaNotes> {
    
    @Select("SELECT DISTINCT category FROM java_notes ORDER BY category")
    List<String> selectCategories();
    
    @Select("SELECT * FROM java_notes WHERE category = #{category} ORDER BY id")
    List<JavaNotes> selectByCategory(String category);
}
