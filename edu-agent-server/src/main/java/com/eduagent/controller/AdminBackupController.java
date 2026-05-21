package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.security.AdminOperation;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin")
public class AdminBackupController {

    @PostMapping("/backup")
    @AdminOperation(value = "创建备份", targetType = "backup")
    public Result<?> createBackup() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", System.currentTimeMillis());
        data.put("filename", "backup_" + new Date() + ".sql");
        data.put("createTime", new Date());
        return Result.ok("备份创建成功", data);
    }

    @GetMapping("/backups")
    public Result<List<Map<String, Object>>> listBackups() {
        return Result.ok(List.of());
    }

    @PostMapping("/backup/{id}/restore")
    @AdminOperation(value = "恢复备份", targetType = "backup")
    public Result<?> restoreBackup(@PathVariable Long id) {
        return Result.ok("恢复成功");
    }

    @GetMapping("/backup/{id}/download")
    public Result<?> downloadBackup(@PathVariable Long id) {
        return Result.ok("download_url_placeholder");
    }

    @DeleteMapping("/backup/{id}")
    @AdminOperation(value = "删除备份", targetType = "backup")
    public Result<?> deleteBackup(@PathVariable Long id) {
        return Result.ok("删除成功");
    }
}
