package com.eduagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eduagent.entity.SystemSetting;
import com.eduagent.mapper.SystemSettingMapper;
import com.eduagent.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl extends ServiceImpl<SystemSettingMapper, SystemSetting> implements SystemSettingService {

    private final SystemSettingMapper settingMapper;

    @Override
    public List<SystemSetting> listAll() {
        return settingMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public void updateSetting(String key, String value) {
        SystemSetting setting = settingMapper.findByKey(key);
        if (setting != null) {
            setting.setSettingValue(value);
            settingMapper.updateById(setting);
        }
    }

    @Override
    public String get(String key) {
        SystemSetting setting = settingMapper.findByKey(key);
        return setting != null ? setting.getSettingValue() : "";
    }
}
