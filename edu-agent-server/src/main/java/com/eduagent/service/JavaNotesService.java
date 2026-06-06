package com.eduagent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eduagent.entity.JavaNotes;
import java.util.List;

public interface JavaNotesService extends IService<JavaNotes> {
    List<String> getCategories();
    List<JavaNotes> getByCategory(String category);
}
