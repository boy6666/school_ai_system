package com.eduagent.service;

import com.eduagent.entity.StudentProfile;
import java.util.Map;

public interface StudentProfileService {
    Map<String, Object> getByUsername(String username);
    Map<String, Object> saveOrUpdate(Long studentId, Map<String, Object> data);
}
