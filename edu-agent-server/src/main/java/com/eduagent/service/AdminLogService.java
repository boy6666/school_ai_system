package com.eduagent.service;

import com.eduagent.entity.AdminLog;
import com.eduagent.repository.AdminLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdminLogService {

    private final AdminLogRepository adminLogRepository;

    public AdminLogService(AdminLogRepository adminLogRepository) {
        this.adminLogRepository = adminLogRepository;
    }

    public Page<AdminLog> getLogs(int page, int pageSize) {
        return adminLogRepository.findAllByOrderByCreateTimeDesc(PageRequest.of(page - 1, pageSize));
    }

    public Map<String, Object> toLogVO(AdminLog log) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", log.getId());
        vo.put("adminId", log.getAdminId());
        vo.put("action", log.getAction());
        vo.put("targetType", log.getTargetType());
        vo.put("targetId", log.getTargetId());
        vo.put("description", log.getDescription());
        vo.put("ipAddress", log.getIpAddress());
        vo.put("createTime", log.getCreateTime());
        return vo;
    }
}
