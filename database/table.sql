-- EduAgent 数据库表结构设计
-- 作者: Ocean
-- 创建时间: 2026-05-20
-- 描述: 包含用户、学生画像、学习资源、任务管理、练习题、辅导对话等核心表

USE `edu_agent`;

-- ================================
-- 1. 用户相关表
-- ================================

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(加密)',
  `nickname` VARCHAR(100) DEFAULT NULL COMMENT '昵称',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `role` ENUM('student', 'teacher', 'admin') NOT NULL DEFAULT 'student' COMMENT '角色',
  `status` ENUM('active', 'inactive', 'locked') NOT NULL DEFAULT 'active' COMMENT '状态',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_username (`username`),
  INDEX idx_email (`email`),
  INDEX idx_role (`role`),
  INDEX idx_status (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ================================
-- 2. 学生画像相关表
-- ================================

-- 学生画像表
CREATE TABLE IF NOT EXISTS `student_profiles` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '画像ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `major` VARCHAR(100) DEFAULT NULL COMMENT '专业',
  `grade` VARCHAR(50) DEFAULT NULL COMMENT '年级',
  `course` VARCHAR(100) DEFAULT NULL COMMENT '课程',
  `topic` VARCHAR(100) DEFAULT NULL COMMENT '当前学习主题',
  `learning_goal` TEXT DEFAULT NULL COMMENT '学习目标',
  `knowledge_base` TEXT DEFAULT NULL COMMENT '知识基础',
  `cognitive_style` TEXT DEFAULT NULL COMMENT '认知风格',
  `pace` VARCHAR(50) DEFAULT NULL COMMENT '学习节奏',
  `weaknesses` JSON DEFAULT NULL COMMENT '薄弱点(JSON数组)',
  `mistake_patterns` JSON DEFAULT NULL COMMENT '易错模式(JSON数组)',
  `resource_preference` JSON DEFAULT NULL COMMENT '资源偏好(JSON数组)',
  `overall_type` ENUM('基础补齐型', '稳定提升型', '进阶拓展型') DEFAULT NULL COMMENT '整体类型',
  `profile_suggestions` JSON DEFAULT NULL COMMENT '画像建议(JSON数组)',
  `last_score` INT DEFAULT NULL COMMENT '最近评估分数',
  `last_suggestion` TEXT DEFAULT NULL COMMENT '最近建议',
  `current_mastery` TEXT DEFAULT NULL COMMENT '当前掌握度',
  `learning_behavior` TEXT DEFAULT NULL COMMENT '学习行为与自主性',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_student_id (`student_id`),
  INDEX idx_topic (`topic`),
  INDEX idx_overall_type (`overall_type`),
  FOREIGN KEY (`student_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生画像表';

-- ================================
-- 3. 学习资源相关表
-- ================================

-- 学习资源表
CREATE TABLE IF NOT EXISTS `resources` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '资源ID',
  `title` VARCHAR(200) NOT NULL COMMENT '资源标题',
  `type` ENUM('文档', 'PPT', '视频', '动画', '题库', '代码案例', '实验项目', '拓展阅读', '思维导图') NOT NULL COMMENT '资源类型',
  `difficulty` ENUM('入门', '基础', '进阶', '高级') NOT NULL DEFAULT '基础' COMMENT '难度等级',
  `description` TEXT DEFAULT NULL COMMENT '资源描述',
  `content` LONGTEXT DEFAULT NULL COMMENT '资源内容',
  `file_url` VARCHAR(500) DEFAULT NULL COMMENT '文件URL',
  `file_size` VARCHAR(50) DEFAULT NULL COMMENT '文件大小',
  `cover` VARCHAR(255) DEFAULT NULL COMMENT '封面图片',
  `author` VARCHAR(100) DEFAULT NULL COMMENT '作者',
  `rating` DECIMAL(3,2) DEFAULT 0.00 COMMENT '评分(0-5)',
  `views` INT DEFAULT 0 COMMENT '浏览量',
  `favorites` INT DEFAULT 0 COMMENT '收藏数',
  `duration` VARCHAR(50) DEFAULT NULL COMMENT '时长',
  `course_id` VARCHAR(50) DEFAULT NULL COMMENT '课程ID',
  `course_name` VARCHAR(100) DEFAULT NULL COMMENT '课程名称',
  `chapter_name` VARCHAR(100) DEFAULT NULL COMMENT '章节名称',
  `chapter_count` INT DEFAULT 0 COMMENT '章节数量',
  `tags` JSON DEFAULT NULL COMMENT '标签(JSON数组)',
  `goals` JSON DEFAULT NULL COMMENT '学习目标(JSON数组)',
  `suitable_for` JSON DEFAULT NULL COMMENT '适合人群(JSON数组)',
  `status` ENUM('draft', 'published', 'archived') NOT NULL DEFAULT 'draft' COMMENT '状态',
  `teacher_id` BIGINT DEFAULT NULL COMMENT '教师ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_type (`type`),
  INDEX idx_difficulty (`difficulty`),
  INDEX idx_course_id (`course_id`),
  INDEX idx_status (`status`),
  INDEX idx_rating (`rating`),
  INDEX idx_views (`views`),
  FULLTEXT idx_content (`title`, `description`, `content`),
  FOREIGN KEY (`teacher_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资源表';

-- 资源章节表
CREATE TABLE IF NOT EXISTS `resource_chapters` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '章节ID',
  `resource_id` BIGINT NOT NULL COMMENT '资源ID',
  `title` VARCHAR(200) NOT NULL COMMENT '章节标题',
  `description` TEXT DEFAULT NULL COMMENT '章节描述',
  `duration` VARCHAR(50) DEFAULT NULL COMMENT '时长',
  `content` LONGTEXT DEFAULT NULL COMMENT '章节内容',
  `order_num` INT DEFAULT 0 COMMENT '排序号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_resource_id (`resource_id`),
  INDEX idx_order_num (`order_num`),
  FOREIGN KEY (`resource_id`) REFERENCES `resources`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源章节表';

-- 资源评价表
CREATE TABLE IF NOT EXISTS `resource_reviews` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
  `resource_id` BIGINT NOT NULL COMMENT '资源ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `score` INT NOT NULL COMMENT '评分(1-5)',
  `content` TEXT DEFAULT NULL COMMENT '评价内容',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_resource_id (`resource_id`),
  INDEX idx_user_id (`user_id`),
  INDEX idx_score (`score`),
  FOREIGN KEY (`resource_id`) REFERENCES `resources`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源评价表';

-- 资源收藏表
CREATE TABLE IF NOT EXISTS `resource_favorites` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `resource_id` BIGINT NOT NULL COMMENT '资源ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  UNIQUE KEY uk_user_resource (`user_id`, `resource_id`),
  INDEX idx_user_id (`user_id`),
  INDEX idx_resource_id (`resource_id`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`resource_id`) REFERENCES `resources`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源收藏表';

-- 学习计划表
CREATE TABLE IF NOT EXISTS `learning_plans` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '计划ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `resource_id` BIGINT NOT NULL COMMENT '资源ID',
  `status` ENUM('pending', 'in_progress', 'completed') NOT NULL DEFAULT 'pending' COMMENT '状态',
  `progress` INT DEFAULT 0 COMMENT '进度(0-100)',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_user_id (`user_id`),
  INDEX idx_resource_id (`resource_id`),
  INDEX idx_status (`status`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`resource_id`) REFERENCES `resources`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习计划表';

-- ================================
-- 4. 学习任务相关表
-- ================================

-- 学习任务表
CREATE TABLE IF NOT EXISTS `learning_tasks` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(200) NOT NULL COMMENT '任务标题',
  `description` TEXT DEFAULT NULL COMMENT '任务描述',
  `course_name` VARCHAR(100) DEFAULT NULL COMMENT '课程名称',
  `chapter_name` VARCHAR(100) DEFAULT NULL COMMENT '章节名称',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `priority` ENUM('high', 'middle', 'low') NOT NULL DEFAULT 'middle' COMMENT '优先级',
  `status` ENUM('todo', 'doing', 'done') NOT NULL DEFAULT 'todo' COMMENT '状态',
  `progress` INT DEFAULT 0 COMMENT '进度(0-100)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_user_id (`user_id`),
  INDEX idx_status (`status`),
  INDEX idx_priority (`priority`),
  INDEX idx_end_time (`end_time`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习任务表';

-- ================================
-- 5. 练习题相关表
-- ================================

-- 练习题表
CREATE TABLE IF NOT EXISTS `practice_questions` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
  `type` ENUM('选择题', '判断题', '填空题', '简答题', '代码题') NOT NULL COMMENT '题目类型',
  `difficulty` ENUM('入门', '基础', '进阶', '高级') NOT NULL DEFAULT '基础' COMMENT '难度',
  `topic` VARCHAR(100) DEFAULT NULL COMMENT '主题',
  `course_id` VARCHAR(50) DEFAULT NULL COMMENT '课程ID',
  `chapter` VARCHAR(100) DEFAULT NULL COMMENT '章节',
  `question` TEXT NOT NULL COMMENT '题目内容',
  `options` JSON DEFAULT NULL COMMENT '选项(JSON数组，选择题用)',
  `answer` TEXT NOT NULL COMMENT '正确答案',
  `analysis` TEXT DEFAULT NULL COMMENT '解析',
  `tags` JSON DEFAULT NULL COMMENT '标签(JSON数组)',
  `score` INT DEFAULT 10 COMMENT '分值',
  `teacher_id` BIGINT DEFAULT NULL COMMENT '出题人ID',
  `status` ENUM('draft', 'published', 'archived') NOT NULL DEFAULT 'draft' COMMENT '状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_type (`type`),
  INDEX idx_difficulty (`difficulty`),
  INDEX idx_topic (`topic`),
  INDEX idx_course_id (`course_id`),
  INDEX idx_status (`status`),
  FOREIGN KEY (`teacher_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='练习题表';

-- 练习记录表
CREATE TABLE IF NOT EXISTS `practice_records` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `user_answer` TEXT DEFAULT NULL COMMENT '用户答案',
  `is_correct` TINYINT(1) DEFAULT NULL COMMENT '是否正确',
  `score` INT DEFAULT 0 COMMENT '得分',
  `time_spent` INT DEFAULT 0 COMMENT '用时(秒)',
  `practice_time` DATETIME NOT NULL COMMENT '练习时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_user_id (`user_id`),
  INDEX idx_question_id (`question_id`),
  INDEX idx_practice_time (`practice_time`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`question_id`) REFERENCES `practice_questions`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='练习记录表';

-- ================================
-- 6. 智能辅导相关表
-- ================================

-- 辅导会话表
CREATE TABLE IF NOT EXISTS `tutor_sessions` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `session_id` VARCHAR(100) NOT NULL UNIQUE COMMENT '会话标识',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
  `topic` VARCHAR(100) DEFAULT NULL COMMENT '主题',
  `start_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `status` ENUM('active', 'ended', 'archived') NOT NULL DEFAULT 'active' COMMENT '状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_user_id (`user_id`),
  INDEX idx_session_id (`session_id`),
  INDEX idx_status (`status`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='辅导会话表';

-- 辅导消息表
CREATE TABLE IF NOT EXISTS `tutor_messages` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
  `session_id` VARCHAR(100) NOT NULL COMMENT '会话标识',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role` ENUM('user', 'assistant') NOT NULL COMMENT '角色',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `message_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_session_id (`session_id`),
  INDEX idx_user_id (`user_id`),
  INDEX idx_message_time (`message_time`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='辅导消息表';

-- 辅导建议表
CREATE TABLE IF NOT EXISTS `tutor_suggestions` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '建议ID',
  `session_id` VARCHAR(100) NOT NULL COMMENT '会话标识',
  `title` VARCHAR(200) NOT NULL COMMENT '建议标题',
  `prompt` TEXT NOT NULL COMMENT '提示词',
  `order_num` INT DEFAULT 0 COMMENT '排序号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_session_id (`session_id`),
  INDEX idx_order_num (`order_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='辅导建议表';

-- ================================
-- 7. 学习记录和报告相关表
-- ================================

-- 学习记录表
CREATE TABLE IF NOT EXISTS `learning_records` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `resource_id` BIGINT DEFAULT NULL COMMENT '资源ID',
  `action` ENUM('view', 'start', 'progress', 'complete') NOT NULL COMMENT '操作类型',
  `duration` INT DEFAULT 0 COMMENT '学习时长(秒)',
  `progress` INT DEFAULT 0 COMMENT '进度(0-100)',
  `record_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `extra_data` JSON DEFAULT NULL COMMENT '额外数据(JSON)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_user_id (`user_id`),
  INDEX idx_resource_id (`resource_id`),
  INDEX idx_action (`action`),
  INDEX idx_record_time (`record_time`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`resource_id`) REFERENCES `resources`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习记录表';

-- 学习报告表
CREATE TABLE IF NOT EXISTS `learning_reports` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报告ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `report_type` ENUM('daily', 'weekly', 'monthly') NOT NULL COMMENT '报告类型',
  `start_date` DATE NOT NULL COMMENT '开始日期',
  `end_date` DATE NOT NULL COMMENT '结束日期',
  `total_study_time` INT DEFAULT 0 COMMENT '总学习时长(分钟)',
  `completed_resources` INT DEFAULT 0 COMMENT '完成资源数',
  `practice_accuracy` DECIMAL(5,2) DEFAULT 0.00 COMMENT '练习正确率',
  `learning_progress` DECIMAL(5,2) DEFAULT 0.00 COMMENT '学习进度',
  `topic_analysis` JSON DEFAULT NULL COMMENT '主题分析(JSON)',
  `suggestions` JSON DEFAULT NULL COMMENT '学习建议(JSON数组)',
  `trends_data` JSON DEFAULT NULL COMMENT '趋势数据(JSON)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_user_id (`user_id`),
  INDEX idx_report_type (`report_type`),
  INDEX idx_start_date (`start_date`),
  INDEX idx_end_date (`end_date`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习报告表';

-- ================================
-- 8. 管理后台相关表
-- ================================

-- 角色表
CREATE TABLE IF NOT EXISTS `roles` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
  `role_name` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
  `role_desc` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
  `permissions` JSON DEFAULT NULL COMMENT '权限列表(JSON数组)',
  `status` ENUM('active', 'inactive') NOT NULL DEFAULT 'active' COMMENT '状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 管理员操作日志表
CREATE TABLE IF NOT EXISTS `admin_logs` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  `admin_id` BIGINT NOT NULL COMMENT '管理员ID',
  `action` VARCHAR(100) NOT NULL COMMENT '操作类型',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '目标类型',
  `target_id` BIGINT DEFAULT NULL COMMENT '目标ID',
  `description` TEXT DEFAULT NULL COMMENT '操作描述',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '用户代理',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_admin_id (`admin_id`),
  INDEX idx_action (`action`),
  INDEX idx_create_time (`create_time`),
  FOREIGN KEY (`admin_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志表';

-- 系统设置表
CREATE TABLE IF NOT EXISTS `system_settings` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设置ID',
  `setting_key` VARCHAR(100) NOT NULL UNIQUE COMMENT '设置键',
  `setting_value` TEXT DEFAULT NULL COMMENT '设置值',
  `setting_desc` VARCHAR(200) DEFAULT NULL COMMENT '设置描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_setting_key (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统设置表';

-- ================================
-- 9. 消息相关表
-- ================================

-- 消息表
CREATE TABLE IF NOT EXISTS `messages` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
  `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
  `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `message_type` ENUM('system', 'notification', 'chat') NOT NULL DEFAULT 'notification' COMMENT '消息类型',
  `status` ENUM('unread', 'read', 'archived') NOT NULL DEFAULT 'unread' COMMENT '状态',
  `send_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_sender_id (`sender_id`),
  INDEX idx_receiver_id (`receiver_id`),
  INDEX idx_status (`status`),
  INDEX idx_send_time (`send_time`),
  FOREIGN KEY (`sender_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`receiver_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ================================
-- 10. 项目相关表
-- ================================

-- 项目表
CREATE TABLE IF NOT EXISTS `projects` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(200) NOT NULL COMMENT '项目标题',
  `description` TEXT DEFAULT NULL COMMENT '项目描述',
  `course_id` VARCHAR(50) DEFAULT NULL COMMENT '课程ID',
  `difficulty` ENUM('入门', '基础', '进阶', '高级') NOT NULL DEFAULT '基础' COMMENT '难度',
  `status` ENUM('draft', 'in_progress', 'completed') NOT NULL DEFAULT 'draft' COMMENT '状态',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `progress` INT DEFAULT 0 COMMENT '进度(0-100)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_user_id (`user_id`),
  INDEX idx_course_id (`course_id`),
  INDEX idx_status (`status`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

-- ================================
-- 11. 课程相关表
-- ================================

-- 课程表
CREATE TABLE IF NOT EXISTS `courses` (
  `id` VARCHAR(50) PRIMARY KEY COMMENT '课程ID',
  `course_name` VARCHAR(200) NOT NULL COMMENT '课程名称',
  `course_code` VARCHAR(50) DEFAULT NULL COMMENT '课程代码',
  `description` TEXT DEFAULT NULL COMMENT '课程描述',
  `teacher_id` BIGINT DEFAULT NULL COMMENT '教师ID',
  `duration` VARCHAR(50) DEFAULT NULL COMMENT '课程时长',
  `difficulty` ENUM('入门', '基础', '进阶', '高级') NOT NULL DEFAULT '基础' COMMENT '难度',
  `tags` JSON DEFAULT NULL COMMENT '标签(JSON数组)',
  `status` ENUM('draft', 'published', 'archived') NOT NULL DEFAULT 'draft' COMMENT '状态',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_course_name (`course_name`),
  INDEX idx_teacher_id (`teacher_id`),
  INDEX idx_status (`status`),
  FOREIGN KEY (`teacher_id`) REFERENCES `users`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

-- ================================
-- 显示创建结果
-- ================================

SELECT 'Database tables created successfully!' AS status;