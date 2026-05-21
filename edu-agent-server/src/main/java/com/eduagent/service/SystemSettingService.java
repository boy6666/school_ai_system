package com.eduagent.service;

import com.eduagent.entity.SystemSetting;
import com.eduagent.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SystemSettingService {

    private final SystemSettingRepository settingRepository;

    public SystemSettingService(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public Map<String, String> getAllSettings() {
        List<SystemSetting> settings = settingRepository.findAll();
        Map<String, String> result = new LinkedHashMap<>();
        for (SystemSetting s : settings) {
            result.put(s.getSettingKey(), s.getSettingValue());
        }
        return result;
    }

    public void updateSettings(Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            SystemSetting setting = settingRepository.findBySettingKey(entry.getKey())
                    .orElseGet(() -> {
                        SystemSetting s = new SystemSetting();
                        s.setSettingKey(entry.getKey());
                        return s;
                    });
            setting.setSettingValue(entry.getValue());
            settingRepository.save(setting);
        }
    }
}
