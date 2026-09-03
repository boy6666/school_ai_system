package com.eduagent.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.learning.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfile> {
}
