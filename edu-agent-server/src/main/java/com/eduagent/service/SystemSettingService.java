package com.eduagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eduagent.entity.SystemSetting;
import java.util.List;

public interface SystemSettingService extends IService<SystemSetting> {
    List<SystemSetting> listAll();
    void updateSetting(String key, String value);
    String get(String key);
}
