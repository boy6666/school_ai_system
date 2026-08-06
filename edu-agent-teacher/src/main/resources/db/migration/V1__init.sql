-- teacher 服务建表（Flyway 版本化迁移，仅首次执行一次，之后由 flyway_schema_history 记录）
-- 库由 database/init-microservice.sql（或部署编排）预建，本脚本只建表，不建库。
-- 去除所有跨库 FK（逻辑引用纯 BIGINT）；deleted 为逻辑删除列（BaseEntity 硬约束，不可缺失）。

CREATE TABLE IF NOT EXISTS `classes` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `name`       VARCHAR(64) NOT NULL,
  `teacher_id` BIGINT   NOT NULL COMMENT '逻辑引用 auth_db.users.id（无 FK）',
  `course`     VARCHAR(64) DEFAULT NULL,
  `semester`   VARCHAR(32) DEFAULT NULL,
  `status`     TINYINT  DEFAULT 1 COMMENT '1启用 0归档',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  INDEX `idx_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

CREATE TABLE IF NOT EXISTS `class_students` (
  `class_id`   BIGINT   NOT NULL,
  `student_id` BIGINT   NOT NULL,
  `joined_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  PRIMARY KEY (`class_id`, `student_id`),
  INDEX `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级学生关系表';

CREATE TABLE IF NOT EXISTS `questions` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `type`        VARCHAR(16) NOT NULL COMMENT 'choice/code/blank',
  `chapter`     VARCHAR(64) DEFAULT NULL,
  `topic`       VARCHAR(64) DEFAULT NULL,
  `content`     TEXT,
  `options`     JSON     COMMENT '选择题选项',
  `answer`      TEXT,
  `explanation` TEXT,
  `difficulty`  VARCHAR(8) DEFAULT 'medium' COMMENT 'easy/medium/hard',
  `creator_id`  BIGINT   NOT NULL,
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  INDEX `idx_chapter` (`chapter`),
  INDEX `idx_topic` (`topic`),
  INDEX `idx_type` (`type`),
  INDEX `idx_creator` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

CREATE TABLE IF NOT EXISTS `assignments` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `class_id`    BIGINT   NOT NULL,
  `title`       VARCHAR(128) NOT NULL,
  `type`        VARCHAR(16) NOT NULL COMMENT 'homework/code',
  `description` TEXT,
  `deadline`    DATETIME DEFAULT NULL,
  `status`      TINYINT  DEFAULT 0 COMMENT '0草稿 1已发布',
  `creator_id`  BIGINT   NOT NULL,
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  INDEX `idx_class` (`class_id`),
  INDEX `idx_creator` (`creator_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业表';

CREATE TABLE IF NOT EXISTS `assignment_items` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `assignment_id` BIGINT   NOT NULL,
  `question_id`   BIGINT   NOT NULL,
  `item_type`     VARCHAR(16) DEFAULT 'choice' COMMENT 'choice/code/blank',
  `score`         INT      DEFAULT 10,
  `order_num`     INT      DEFAULT 0,
  `deleted`       TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  INDEX `idx_assignment` (`assignment_id`),
  INDEX `idx_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业题目项表';

CREATE TABLE IF NOT EXISTS `grades` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `assignment_id` BIGINT   NOT NULL,
  `student_id`    BIGINT   NOT NULL,
  `item_id`       BIGINT   NOT NULL,
  `item_type`     VARCHAR(16) DEFAULT 'choice',
  `language`      VARCHAR(16) DEFAULT NULL COMMENT 'code 题语言，如 java',
  `submission`    TEXT     COMMENT '学生提交内容/代码',
  `run_result`    JSON     COMMENT '运行结果（来自 code-service）',
  `static_report` JSON     COMMENT '静态检查报告（来自 code-service）',
  `ai_report`     JSON     COMMENT 'AI 建议（来自 code-service / ai）',
  `score`         INT      DEFAULT 0,
  `status`        TINYINT  DEFAULT 0 COMMENT '0待批 1已批',
  `comment`       TEXT     COMMENT '教师评语/复核',
  `graded_at`     DATETIME DEFAULT NULL,
  `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`       TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  UNIQUE KEY `uk_stu_item` (`assignment_id`, `student_id`, `item_id`),
  INDEX `idx_assignment` (`assignment_id`),
  INDEX `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';
