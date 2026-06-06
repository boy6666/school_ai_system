package com.eduagent.controller;

import com.eduagent.common.Result;
import com.eduagent.agent.AiClient;
import com.eduagent.entity.Resource;
import com.eduagent.entity.StudentProfile;
import com.eduagent.mapper.ResourceMapper;
import com.eduagent.mapper.StudentProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceMapper resourceMapper;
    private final AiClient aiClient;
    private final StudentProfileMapper studentProfileMapper;

    private static final Map<String, String> TYPE_MAP = Map.of(
        "mindmap", "思维导图",
        "quiz", "题库",
        "reading", "拓展阅读",
        "code", "代码案例"
    );
    private static final Map<String, String> DIFF_MAP = Map.of(
        "简单", "入门",
        "适合", "基础",
        "困难", "进阶"
    );
    // 反向映射：数据库难度 → 前端展示
    private static final Map<String, String> DIFF_REVERSE = Map.of(
        "入门", "简单",
        "基础", "适合",
        "进阶", "困难"
    );
    // 难度阶梯：用于上下调整
    private static final List<String> DIFF_LEVELS = List.of("入门", "基础", "进阶");

    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        Long studentId = body.get("studentId") instanceof Number
            ? ((Number) body.get("studentId")).longValue()
            : Long.parseLong(body.get("studentId").toString());
        String type = (String) body.get("type");
        String chapterName = (String) body.get("chapterName");
        String title = (String) body.get("title");
        String difficulty = (String) body.get("difficulty");
        String dbType = TYPE_MAP.getOrDefault(type, "文档");
        String dbDiff = DIFF_MAP.getOrDefault(difficulty, "基础");

        log.info("===== [资源生成] 请求开始 =====");
        log.info("studentId={}, type={}, chapterName={}, title={}, difficulty={}", studentId, type, chapterName, title, difficulty);
        log.info("映射后: dbType={}, dbDiff={}", dbType, dbDiff);

        // 查是否已存在
        Resource existing = resourceMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Resource>()
                .eq(Resource::getStudentId, studentId)
                .eq(Resource::getChapterName, chapterName)
                .eq(Resource::getType, dbType)
                .eq(Resource::getDifficulty, dbDiff)
                .last("LIMIT 1")
        );
        if (existing != null) {
            log.info("✅ 数据库已存在该资源, id={}, 直接返回", existing.getId());
            return Result.success(Map.of("id", existing.getId(), "content", existing.getContent(), "exists", true));
        }
        log.info("数据库未命中，准备调 AI 生成");

        // 查画像
        Map<String, Object> profileMap = new HashMap<>();
        try {
            StudentProfile sp = studentProfileMapper.findByStudentId(studentId);
            if (sp != null) {
                log.info("✅ 查到学生画像, id={}, course={}, topic={}, pace={}, goal={}",
                    sp.getId(), sp.getCourse(), sp.getTopic(), sp.getPace(), sp.getLearningGoal());
                profileMap.put("course", sp.getCourse());
                profileMap.put("topic", sp.getTopic());
                profileMap.put("pace", sp.getPace());
                profileMap.put("learning_goal", sp.getLearningGoal());
            } else {
                log.warn("⚠️ 未找到学生画像, studentId={}", studentId);
            }
        } catch (Exception e) {
            log.error("❌ 查画像异常: {}", e.getMessage(), e);
        }

        // 调 AI
        Map<String, Object> aiBody = new HashMap<>();
        aiBody.put("student_id", String.valueOf(studentId));
        aiBody.put("type", type);
        aiBody.put("title", title);
        aiBody.put("chapter", chapterName);
        aiBody.put("difficulty", difficulty);
        aiBody.put("profile", profileMap);
        aiBody.put("mode", "generate_resource");
        String prompt;
        if ("mindmap".equals(type)) {
            prompt = "请为「" + title + "」生成一份思维导图，使用 Markdown 标题格式（# ## ###），难度：" + difficulty
                + "。只返回 Markdown 内容，不要额外说明。";
        } else if ("quiz".equals(type)) {
            prompt = "请为「" + title + "」生成练习题，难度：" + difficulty + "。包含题目和答案。";
        } else if ("reading".equals(type)) {
            prompt = "请为「" + title + "」生成拓展阅读材料，难度：" + difficulty + "。约300字。";
        } else if ("code".equals(type)) {
            prompt = "请为「" + title + "」生成Java代码案例，难度：" + difficulty + "。可运行，带注释。";
        } else {
            prompt = "生成" + title + "的" + dbType + "，难度：" + difficulty;
        }
        aiBody.put("prompt", prompt);

        log.info("🚀 正在调 AI 生成...");
        log.debug("发送给 AI 的 body: student_id={}, type={}, chapter={}, difficulty={}, profile={}",
            studentId, type, chapterName, difficulty, profileMap);
        log.debug("发送给 AI 的 prompt(前200字): {}", prompt.length() > 200 ? prompt.substring(0, 200) + "..." : prompt);

        String aiResult = aiClient.post("/resource/generate", aiBody);
        log.info("📥 AI 原始返回(前200字): {}", aiResult != null && aiResult.length() > 200 ? aiResult.substring(0, 200) + "..." : aiResult);

        String content = "";
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> aiMap = om.readValue(aiResult, Map.class);
            if (aiMap.containsKey("content")) {
                content = (String) aiMap.get("content");
                log.info("✅ AI 返回 content 长度: {} 字", content.length());
            } else {
                log.warn("⚠️ AI 返回中没有 content 字段: {}", aiMap.keySet());
            }
        } catch (Exception e) {
            log.error("❌ 解析 AI 返回失败: {}", e.getMessage());
            log.error("原始返回: {}", aiResult);
        }

        // 存 DB
        Resource r = new Resource();
        r.setStudentId(studentId);
        r.setTitle(title + " - " + dbType);
        r.setType(dbType);
        r.setDifficulty(dbDiff);
        r.setChapterName(chapterName);
        r.setContent(content);
        r.setStatus("published");
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        int insertResult = resourceMapper.insert(r);
        log.info("💾 DB 插入结果: affected={}, newId={}", insertResult, r.getId());

        log.info("===== [资源生成] 请求结束, 返回 content 长度: {} =====", content.length());
        return Result.success(Map.of("id", r.getId(), "content", content, "exists", false));
    }

    /**
     * 调整资源难度：用户反馈内容太简单/太困难时，AI 重新生成
     * direction=up:  用户说"简单" → 生成更难的内容
     * direction=down: 用户说"困难" → 生成更简单的内容
     *
     * 关键流程：调 AI → INSERT 到 DB → SELECT 读出 → 返回前端
     * AI 结果不直接返回前端，必须经过数据库
     */
    @PostMapping("/adjust-difficulty")
    public Result<Map<String, Object>> adjustDifficulty(@RequestBody Map<String, Object> body) {
        Long studentId = body.get("studentId") instanceof Number
            ? ((Number) body.get("studentId")).longValue()
            : Long.parseLong(body.get("studentId").toString());
        String type = (String) body.get("type");
        String chapterName = (String) body.get("chapterName");
        String title = (String) body.get("title");
        String direction = (String) body.get("direction"); // "up" 或 "down"
        String currentDifficulty = (String) body.get("currentDifficulty"); // "简单"/"适合"/"困难"

        log.info("===== [难度调整] 请求开始 =====");
        log.info("studentId={}, type={}, chapterName={}, title={}, direction={}, currentDifficulty={}",
            studentId, type, chapterName, title, direction, currentDifficulty);

        String dbType = TYPE_MAP.getOrDefault(type, "文档");
        String currentDbDiff = DIFF_MAP.getOrDefault(currentDifficulty, "基础");

        // 计算新难度
        int idx = DIFF_LEVELS.indexOf(currentDbDiff);
        if (idx < 0) idx = 1; // 默认基础
        if ("up".equals(direction)) {
            idx = Math.min(idx + 1, DIFF_LEVELS.size() - 1); // 用户说简单→生成更难
            log.info("⬆ direction=up (用户说太简单), 难度升级: {}→{}", currentDbDiff, DIFF_LEVELS.get(idx));
        } else {
            idx = Math.max(idx - 1, 0); // 用户说困难→生成更简单
            log.info("⬇ direction=down (用户说太困难), 难度降级: {}→{}", currentDbDiff, DIFF_LEVELS.get(idx));
        }
        String newDbDiff = DIFF_LEVELS.get(idx);
        String newDisplayDiff = DIFF_REVERSE.get(newDbDiff);

        // 注意：用户点了「简单/困难」必须重新调 AI 生成
        // 不能使用缓存，因为画像可能已更新，需要用最新画像重新生成
        log.info("🎯 用户手动调整难度，直接调 AI 重新生成（不用缓存）");

        // 查画像
        Map<String, Object> profileMap = new HashMap<>();
        try {
            StudentProfile sp = studentProfileMapper.findByStudentId(studentId);
            if (sp != null) {
                log.info("✅ 查到学生画像, id={}, course={}, topic={}, pace={}, goal={}, weaknesses={}",
                    sp.getId(), sp.getCourse(), sp.getTopic(), sp.getPace(),
                    sp.getLearningGoal(), sp.getWeaknesses());
                profileMap.put("course", sp.getCourse());
                profileMap.put("topic", sp.getTopic());
                profileMap.put("pace", sp.getPace());
                profileMap.put("learning_goal", sp.getLearningGoal());
                profileMap.put("weaknesses", sp.getWeaknesses());
                profileMap.put("knowledge_base", sp.getKnowledgeBase());
            } else {
                log.warn("⚠️ 未找到学生画像, studentId={}", studentId);
            }
        } catch (Exception e) {
            log.error("❌ 查画像异常: {}", e.getMessage(), e);
        }

        // 构造 AI 请求 —— 必须携带画像
        Map<String, Object> aiBody = new HashMap<>();
        aiBody.put("student_id", String.valueOf(studentId));
        aiBody.put("type", type);
        aiBody.put("title", title);
        aiBody.put("chapter", chapterName);
        aiBody.put("difficulty", newDisplayDiff);
        aiBody.put("profile", profileMap);
        aiBody.put("mode", "generate_resource");

        // 带画像信息的 prompt
        String profileContext = "";
        if (!profileMap.isEmpty()) {
            profileContext = "学生画像信息：";
            if (profileMap.get("course") != null) profileContext += "课程=" + profileMap.get("course") + "、";
            if (profileMap.get("topic") != null) profileContext += "当前主题=" + profileMap.get("topic") + "、";
            if (profileMap.get("pace") != null) profileContext += "学习节奏=" + profileMap.get("pace") + "、";
            if (profileMap.get("learning_goal") != null) profileContext += "学习目标=" + profileMap.get("learning_goal") + "、";
            if (profileMap.get("weaknesses") != null) profileContext += "薄弱点=" + profileMap.get("weaknesses") + "、";
            if (profileMap.get("knowledge_base") != null) profileContext += "知识基础=" + profileMap.get("knowledge_base");
        }

        String prompt;
        if ("mindmap".equals(type)) {
            prompt = profileContext + "\n\n请为「" + title + "」生成一份思维导图，使用 Markdown 标题格式（# ## ###），"
                + "难度调整为：" + newDisplayDiff + "。根据学生画像调整内容的侧重点和深度。"
                + "只返回 Markdown 内容，不要额外说明。";
        } else if ("quiz".equals(type)) {
            prompt = profileContext + "\n\n请为「" + title + "」生成练习题，"
                + "难度调整为：" + newDisplayDiff + "。包含题目和答案。根据学生薄弱点调整题目方向。";
        } else if ("reading".equals(type)) {
            prompt = profileContext + "\n\n请为「" + title + "」生成拓展阅读材料，"
                + "难度调整为：" + newDisplayDiff + "。约300字。根据学生画像调整内容深度。";
        } else if ("code".equals(type)) {
            prompt = profileContext + "\n\n请为「" + title + "」生成Java代码案例，"
                + "难度调整为：" + newDisplayDiff + "。可运行，带注释。根据学生水平调整案例复杂度。";
        } else {
            prompt = profileContext + "\n\n生成" + title + "的" + dbType + "，难度：" + newDisplayDiff;
        }
        aiBody.put("prompt", prompt);

        log.info("🚀 正在调 AI 按新难度生成...");
        log.debug("发送给 AI 的 profile: {}", profileMap);
        log.debug("发送给 AI 的 prompt(前200字): {}", prompt.length() > 200 ? prompt.substring(0, 200) + "..." : prompt);

        // 调 AI
        String aiResult = aiClient.post("/resource/generate", aiBody);
        log.info("📥 AI 原始返回(前200字): {}", aiResult != null && aiResult.length() > 200 ? aiResult.substring(0, 200) + "..." : aiResult);

        String content = "";
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> aiMap = om.readValue(aiResult, Map.class);
            if (aiMap.containsKey("content")) {
                content = (String) aiMap.get("content");
                log.info("✅ AI 返回 content 长度: {} 字", content.length());
            } else {
                log.warn("⚠️ AI 返回中没有 content 字段, keys={}", aiMap.keySet());
            }
        } catch (Exception e) {
            log.error("❌ 解析 AI 返回失败: {}", e.getMessage());
            log.error("原始返回: {}", aiResult);
        }

        // 先存入 DB（AI 结果不直接返回，必须经过数据库）
        Resource r = new Resource();
        r.setStudentId(studentId);
        r.setTitle(title + " - " + dbType + "(" + newDisplayDiff + ")");
        r.setType(dbType);
        r.setDifficulty(newDbDiff);
        r.setChapterName(chapterName);
        r.setContent(content);
        r.setStatus("published");
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        int insertResult = resourceMapper.insert(r);
        log.info("💾 DB 插入结果: affected={}, newId={}", insertResult, r.getId());

        // 从 DB 读出再返回（确保数据已落地）
        Resource saved = resourceMapper.selectById(r.getId());
        String finalContent = saved != null ? saved.getContent() : content;
        log.info("📖 DB 回查: id={}, content长度={}, 与原始content一致={}",
            r.getId(), finalContent != null ? finalContent.length() : 0, finalContent != null && finalContent.equals(content));

        log.info("===== [难度调整] 请求结束, newDbDiff={}, newDisplayDiff={}, content长度={} =====", newDbDiff, newDisplayDiff, finalContent != null ? finalContent.length() : 0);
        // 使用 HashMap（避免 Map.of 对 null 值抛 NPE）
        Map<String, Object> result = new HashMap<>();
        result.put("id", r.getId());
        result.put("content", finalContent != null ? finalContent : "");
        result.put("difficulty", newDisplayDiff != null ? newDisplayDiff : currentDifficulty);
        result.put("exists", false);
        return Result.success(result);
    }

    @GetMapping
    public Result<List<Resource>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String chapterName,
            @RequestParam(required = false) String difficulty) {
        Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
        List<Resource> list = resourceMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Resource>()
                .eq(Resource::getStudentId, userId)
                .eq(type != null, Resource::getType, type)
                .eq(chapterName != null, Resource::getChapterName, chapterName)
                .eq(difficulty != null, Resource::getDifficulty, difficulty)
                .orderByDesc(Resource::getUpdateTime)
        );
        return Result.success(list);
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Resource r = new Resource();
        r.setTitle((String) body.get("title"));
        r.setType((String) body.get("type"));
        r.setDifficulty((String) body.get("difficulty"));
        r.setContent((String) body.get("content"));
        r.setChapterName((String) body.get("chapterName"));
        r.setCourseName((String) body.get("courseName"));
        Object sid = body.get("studentId");
        if (sid instanceof Number) r.setStudentId(((Number) sid).longValue());
        r.setStatus("published");
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        resourceMapper.insert(r);
        return Result.success(Map.of("id", r.getId(), "status", "created"));
    }

    @GetMapping("/{id}")
    public Result<Resource> getById(@PathVariable Long id) {
        Resource r = resourceMapper.selectById(id);
        return Result.success(r);
    }
}
