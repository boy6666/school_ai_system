package com.eduagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfile> {
}
