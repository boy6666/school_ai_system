package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.entity.JavaNotes;
import com.eduagent.service.JavaNotesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class JavaNotesController {

    private final JavaNotesService javaNotesService;

    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> getCategories() {
        List<String> cats = javaNotesService.getCategories();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < cats.size(); i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", i + 1);
            item.put("category", cats.get(i));
            // 提取章节名（去掉数字前缀）
            String label = cats.get(i).replaceFirst("\\d+_", "").replace("_", " ");
            item.put("label", label);
            List<JavaNotes> notes = javaNotesService.getByCategory(cats.get(i));
            item.put("count", notes.size());
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping
    public Result<List<Map<String, Object>>> getNotes(@RequestParam String category) {
        List<JavaNotes> notes = javaNotesService.getByCategory(category);
        List<Map<String, Object>> result = new ArrayList<>();
        for (JavaNotes n : notes) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", n.getId());
            item.put("title", n.getTitle());
            item.put("category", n.getCategory());
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<JavaNotes> getNote(@PathVariable Integer id) {
        JavaNotes note = javaNotesService.getById(id);
        if (note == null) {
            return Result.success(null);
        }
        return Result.success(note);
    }
}
