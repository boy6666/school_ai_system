package com.eduagent.controller;

import com.eduagent.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/reports")
public class ReportController {

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        Map<String, Object> item = new HashMap<>();
        item.put("message", "reports stub - pending");
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(item);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("message", "reports stub - pending");
        return Result.success(map);
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "created");
        map.put("message", "reports stub - pending");
        return Result.success(map);
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("status", "updated");
        return Result.success(map);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return Result.success();
    }
}
