package com.eduagent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.eduagent.entity.AgentConfig;

public interface AgentConfigService extends IService<AgentConfig> {
    Page<AgentConfig> listAgents(int page, int pageSize, String keyword);
}
