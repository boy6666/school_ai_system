package com.eduagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eduagent.entity.SystemSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemSettingMapper extends BaseMapper<SystemSetting> {
    @Select("SELECT * FROM system_settings WHERE setting_key = #{key}")
    SystemSetting findByKey(String key);
}
