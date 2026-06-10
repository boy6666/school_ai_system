package com.eduagent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eduagent.common.Result;
import com.eduagent.entity.Resource;
import com.eduagent.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceControllerTest {

    @Mock private ResourceService resourceService;

    @InjectMocks
    private ResourceController controller;

    @Test
    void list_ShouldReturnPagedResources() {
        Page<Resource> page = new Page<>();
        page.setRecords(List.of(new Resource()));
        page.setTotal(1);
        page.setCurrent(1);
        page.setSize(20);
        when(resourceService.listResources(1, 20, null, null, null)).thenReturn(page);

        Result<Map<String, Object>> result = controller.list(1, 20, null, null, null);

        assertEquals(200, result.getCode());
        assertEquals(1L, result.getData().get("total"));
    }

    @Test
    void getById_ShouldReturnResource_WhenExists() {
        Resource r = new Resource();
        r.setId(1L);
        r.setTitle("Java思维导图");
        r.setType("mindmap");
        r.setViews(10);
        when(resourceService.getById(1L)).thenReturn(r);

        Result<Resource> result = controller.getById(1L);

        assertEquals(200, result.getCode());
        assertEquals("Java思维导图", result.getData().getTitle());
        assertEquals(11, result.getData().getViews().intValue()); // views incremented
        verify(resourceService).updateById(r);
    }

    @Test
    void getById_ShouldReturn404_WhenNotExists() {
        when(resourceService.getById(999L)).thenReturn(null);

        Result<Resource> result = controller.getById(999L);

        assertEquals(404, result.getCode());
    }

    @Test
    void getByChapter_ShouldReturnResources() {
        List<Resource> resources = List.of(new Resource(), new Resource());
        when(resourceService.listByChapterId(1L)).thenReturn(resources);

        Result<List<Resource>> result = controller.getByChapter(1L);

        assertEquals(200, result.getCode());
        assertEquals(2, result.getData().size());
    }

    @Test
    void delete_ShouldCallService() {
        when(resourceService.removeById(1L)).thenReturn(true);

        Result<Void> result = controller.delete(1L);

        assertEquals(200, result.getCode());
        verify(resourceService).removeById(1L);
    }
}
