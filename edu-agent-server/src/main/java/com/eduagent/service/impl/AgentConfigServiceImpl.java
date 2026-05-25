package com.eduagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eduagent.entity.AgentConfig;
import com.eduagent.mapper.AgentConfigMapper;
import com.eduagent.service.AgentConfigService;
import org.springframework.stereotype.Service;

@Service
public class AgentConfigServiceImpl extends ServiceImpl<AgentConfigMapper, AgentConfig> implements AgentConfigService {

    @Override
    public Page<AgentConfig> listAgents(int page, int pageSize, String keyword) {
        LambdaQueryWrapper<AgentConfig> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(AgentConfig::getName, keyword).or().like(AgentConfig::getType, keyword);
        }
        wrapper.orderByDesc(AgentConfig::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }
}
