package com.eduagent.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eduagent.entity.JavaNotes;
import com.eduagent.mapper.JavaNotesMapper;
import com.eduagent.service.JavaNotesService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class JavaNotesServiceImpl extends ServiceImpl<JavaNotesMapper, JavaNotes> implements JavaNotesService {
    @Override
    public List<String> getCategories() {
        return baseMapper.selectCategories();
    }
    
    @Override
    public List<JavaNotes> getByCategory(String category) {
        return baseMapper.selectByCategory(category);
    }
}
