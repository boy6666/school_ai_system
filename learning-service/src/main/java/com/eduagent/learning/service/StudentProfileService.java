package com.eduagent.learning.service;

import com.eduagent.learning.entity.StudentProfile;
import com.eduagent.learning.mapper.StudentProfileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StudentProfileService {

    @Autowired
    private StudentProfileMapper studentProfileMapper;

    public StudentProfile getByUserId(Long userId) {
        StudentProfile profile = studentProfileMapper.selectById(userId);
        if (profile == null) {
            profile = new StudentProfile();
            profile.setUserId(userId);
            profile.setCourse("Java 微服务");
            profile.setTopic("Spring Boot");
            profile.setKnowledgeBase("熟悉基本语法");
            profile.setWeaknesses("多线程");
            profile.setPace("medium");
            profile.setResourcePreference("视频");
            profile.setLastScore(80);
        }
        return profile;
    }
}
