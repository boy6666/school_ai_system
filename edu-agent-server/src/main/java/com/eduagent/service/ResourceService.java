package com.eduagent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.eduagent.entity.Resource;

public interface ResourceService extends IService<Resource> {
    Page<Resource> listResources(int page, int pageSize, String keyword, String type, String status);
}
