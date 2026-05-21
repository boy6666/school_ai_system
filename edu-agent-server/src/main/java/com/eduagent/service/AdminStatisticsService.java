package com.eduagent.service;

import com.eduagent.entity.User;
import com.eduagent.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdminStatisticsService {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final CourseRepository courseRepository;

    public AdminStatisticsService(UserRepository userRepository,
                                  ResourceRepository resourceRepository,
                                  CourseRepository courseRepository) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.courseRepository = courseRepository;
    }

    public Map<String, Object> getOverview() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalStudents", userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.UserRole.student).count());
        stats.put("totalTeachers", userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.UserRole.teacher).count());
        stats.put("totalResources", resourceRepository.count());
        stats.put("totalCourses", courseRepository.count());
        stats.put("activeUsers", userRepository.findAll().stream()
                .filter(u -> u.getStatus() == User.UserStatus.active).count());
        return stats;
    }

    public Map<String, Object> getUserGrowth(String period) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> growth = new ArrayList<>();
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("date", "2026-05");
        point.put("count", userRepository.count());
        growth.add(point);
        data.put("growth", growth);
        data.put("period", period);
        return data;
    }

    public Map<String, Object> getLearningData(String period) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalStudyTime", 0);
        data.put("avgAccuracy", 0);
        data.put("activeStudents", 0);
        data.put("period", period);
        return data;
    }
}
