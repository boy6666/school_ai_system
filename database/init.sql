-- ============================================================
-- EduAgent 数据库初始化脚本
-- 使用: docker exec -i edu-mysql mysql -uroot -proot123456 edu_agent < database/init.sql
-- ============================================================

-- 创建数据库（如果尚未存在）
CREATE DATABASE IF NOT EXISTS edu_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE edu_agent;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(Bcrypt加密)',
    `nickname` VARCHAR(100) COMMENT '昵称',
    `email` VARCHAR(100) COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',
    `avatar` VARCHAR(500) COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'student' COMMENT '角色: student/admin',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/inactive',
    `onboarded` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否完成引导',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) COMMENT '最后登录IP',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_username` (`username`),
    INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 学生画像表
-- ============================================================
CREATE TABLE IF NOT EXISTS `student_profiles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL UNIQUE COMMENT '关联用户ID',
    `course` VARCHAR(100) COMMENT '课程方向',
    `topic` VARCHAR(200) COMMENT '学习主题',
    `learning_goal` TEXT COMMENT '学习目标',
    `knowledge_base` TEXT COMMENT '知识基础',
    `weaknesses` JSON COMMENT '薄弱点',
    `mistake_patterns` JSON COMMENT '错误模式',
    `resource_preference` JSON COMMENT '资源偏好',
    `cognitive_style` VARCHAR(50) COMMENT '认知风格',
    `pace` VARCHAR(50) COMMENT '学习节奏',
    `last_score` INT COMMENT '最近得分',
    `profile_data` JSON COMMENT '画像数据',
    `profile_suggestions` TEXT COMMENT '画像建议',
    `last_suggestion` TEXT COMMENT '最近建议',
    `overall_type` VARCHAR(50) COMMENT '整体类型',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`student_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生画像表';

-- ============================================================
-- 3. 学习路径表
-- ============================================================
CREATE TABLE IF NOT EXISTS `learning_paths` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `steps` JSON NOT NULL COMMENT '学习步骤',
    `progress` INT DEFAULT 0 COMMENT '整体进度%',
    `pace` VARCHAR(50) COMMENT '节奏',
    `goal` VARCHAR(200) COMMENT '目标',
    `suggestions` TEXT COMMENT '建议',
    `recommendations` TEXT COMMENT '推荐资源',
    `exam_advice` TEXT COMMENT '考试建议',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`student_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_student_path` (`student_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习路径表';

-- ============================================================
-- 4. 学习日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `study_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `date` DATE NOT NULL COMMENT '学习日期',
    `duration` INT NOT NULL COMMENT '学习时长(分钟)',
    `modules` JSON COMMENT '学习的模块',
    `description` TEXT COMMENT '学习描述',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`student_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_student_date` (`student_id`, `date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习日志表';

-- ============================================================
-- 5. 学习资源表
-- ============================================================
CREATE TABLE IF NOT EXISTS `learning_resources` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL COMMENT '资源标题',
    `type` VARCHAR(50) NOT NULL COMMENT '类型: video/article/code/mindmap',
    `content` TEXT COMMENT '资源内容/URL',
    `tags` JSON COMMENT '标签',
    `status` VARCHAR(20) DEFAULT 'unread' COMMENT '状态: unread/reading/completed',
    `completion_rate` INT DEFAULT 0 COMMENT '完成率%',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`student_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_student_resources` (`student_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资源表';

-- ============================================================
-- 6. 练习记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `exercise_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `exercise_type` VARCHAR(50) NOT NULL COMMENT '练习类型',
    `score` INT COMMENT '得分',
    `total_questions` INT COMMENT '总题数',
    `correct_count` INT COMMENT '正确数',
    `wrong_questions` JSON COMMENT '错题',
    `duration` INT COMMENT '用时(秒)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`student_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_student_exercise` (`student_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='练习记录表';

-- ============================================================
-- 7. 对话记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `conversations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `title` VARCHAR(200) COMMENT '对话标题',
    `messages` JSON NOT NULL COMMENT '对话内容',
    `summary` TEXT COMMENT '对话摘要',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`student_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_student_conv` (`student_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话记录表';

-- ============================================================
-- 8. 测验记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `quiz_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `quiz_type` VARCHAR(50) NOT NULL COMMENT '测验类型',
    `questions` JSON COMMENT '题目',
    `answers` JSON COMMENT '用户答案',
    `score` INT COMMENT '得分',
    `correct_count` INT COMMENT '正确数',
    `wrong_analysis` JSON COMMENT '错题分析',
    `duration` INT COMMENT '用时(秒)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`student_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    INDEX `idx_student_quiz` (`student_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测验记录表';

-- ============================================================
-- 9. 管理员统计缓存表
-- ============================================================
CREATE TABLE IF NOT EXISTS `admin_stats_cache` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `stat_type` VARCHAR(50) NOT NULL COMMENT '统计类型',
    `stat_data` JSON NOT NULL COMMENT '统计数据',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_stat_type` (`stat_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员统计缓存表';

-- ============================================================
-- 插入默认管理员账号 (密码: admin123)
-- ============================================================
INSERT INTO `users` (`username`, `password`, `nickname`, `role`, `status`, `onboarded`)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye.cMB9C.kLCErGFmIj/.LFsR.G9Q0zWy', '管理员', 'admin', 'active', 1)
ON DUPLICATE KEY UPDATE `nickname` = '管理员';

-- ============================================================
-- 插入测试学生账号 (密码: student123)
-- ============================================================
INSERT INTO `users` (`username`, `password`, `nickname`, `role`, `status`, `onboarded`)
VALUES ('teststudent', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '测试学生', 'student', 'active', 1)
ON DUPLICATE KEY UPDATE `nickname` = '测试学生';