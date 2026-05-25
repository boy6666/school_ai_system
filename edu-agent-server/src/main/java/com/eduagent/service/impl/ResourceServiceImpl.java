package com.eduagent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eduagent.entity.Resource;
import com.eduagent.mapper.ResourceMapper;
import com.eduagent.service.ResourceService;
import org.springframework.stereotype.Service;

@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements ResourceService {

    @Override
    public Page<Resource> listResources(int page, int pageSize, String keyword, String type, String status) {
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Resource::getTitle, keyword);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Resource::getType, type);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Resource::getStatus, status);
        }
        wrapper.orderByDesc(Resource::getCreateTime);
        return page(new Page<>(page, pageSize), wrapper);
    }
}
