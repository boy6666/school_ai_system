-- learning 服务建表（Flyway 版本化迁移，仅首次执行一次，之后由 flyway_schema_history 记录）
-- 库由 database/init-microservice.sql（或部署编排）预建，本脚本只建表，不建库。
-- 去除所有跨库 FK（逻辑引用纯 BIGINT）；deleted 为逻辑删除列（BaseEntity 硬约束，不可缺失）。

CREATE TABLE IF NOT EXISTS `student_profiles` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `student_id`         BIGINT       NOT NULL COMMENT '逻辑关联 auth_db.users.id（无 FK）',
  `class_id`           BIGINT       DEFAULT NULL COMMENT '逻辑归属班级 teacher_db.classes.id（无 FK）',
  `major`              VARCHAR(100) DEFAULT NULL,
  `grade`              VARCHAR(50)  DEFAULT NULL,
  `course`             VARCHAR(100) DEFAULT NULL,
  `topic`              VARCHAR(100) DEFAULT NULL,
  `learning_goal`      TEXT,
  `knowledge_base`     TEXT,
  `cognitive_style`    VARCHAR(50)  DEFAULT NULL,
  `pace`               VARCHAR(50)  DEFAULT NULL,
  `weaknesses`         JSON,
  `mistake_patterns`   JSON,
  `resource_preference` JSON,
  `overall_type`       VARCHAR(50)  DEFAULT NULL,
  `last_score`         INT          DEFAULT NULL,
  `profile_data`       JSON         COMMENT '六维画像 JSON，结构对齐 ai-service profile_schema',
  `profile_suggestions` TEXT,
  `last_suggestion`    TEXT,
  `profile_complete`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '0未完成引导 1已完成',
  `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`            TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  UNIQUE KEY `uk_student_id` (`student_id`),
  INDEX `idx_class_id` (`class_id`),
  INDEX `idx_topic` (`topic`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生画像表';

CREATE TABLE IF NOT EXISTS `learning_paths` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `student_id`   BIGINT   NOT NULL,
  `steps`        LONGTEXT COMMENT '学习路径 JSON（LearningPathVO 序列化）',
  `progress`     INT      DEFAULT 0 COMMENT '整体进度%',
  `pace`         VARCHAR(50) DEFAULT 'medium',
  `goal`         TEXT,
  `suggestions`  TEXT,
  `recommendations` TEXT,
  `exam_advice`  TEXT,
  `status`       VARCHAR(20) DEFAULT 'active',
  `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  INDEX `idx_student_status` (`student_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习路径表';

CREATE TABLE IF NOT EXISTS `learning_tasks` (
  `id`           BIGINT              NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id`      BIGINT              NOT NULL,
  `title`        VARCHAR(200)        NOT NULL,
  `description`  TEXT,
  `course_name`  VARCHAR(100)        DEFAULT NULL,
  `chapter_name` VARCHAR(100)        DEFAULT NULL,
  `stage`        VARCHAR(20)         DEFAULT 'today',
  `start_time`   DATETIME            DEFAULT NULL,
  `end_time`     DATETIME            DEFAULT NULL,
  `priority`     ENUM('high','middle','low') DEFAULT 'middle',
  `status`       ENUM('todo','doing','done') DEFAULT 'todo',
  `progress`     INT                 DEFAULT 0,
  `create_time`  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT             NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习任务表';

CREATE TABLE IF NOT EXISTS `learning_path_history` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `student_id` BIGINT   NOT NULL,
  `goal`       TEXT,
  `path_data`  JSON,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted`    TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  INDEX `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习路径历史表';

CREATE TABLE IF NOT EXISTS `study_logs` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `student_id`   BIGINT   NOT NULL,
  `module`       VARCHAR(20) DEFAULT NULL COMMENT 'mindmap/quiz/reading/code',
  `duration_sec` INT      DEFAULT 0,
  `chapter_id`   INT      DEFAULT NULL,
  `note_id`      INT      DEFAULT NULL,
  `created_at`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted`      TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  INDEX `idx_student` (`student_id`),
  INDEX `idx_date` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习日志表';

CREATE TABLE IF NOT EXISTS `quiz_answer` (
  `id`             BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `student_id`     BIGINT   NOT NULL,
  `resource_id`    BIGINT   DEFAULT NULL COMMENT '逻辑引用 resource_db（无 FK）',
  `question`       TEXT,
  `question_type`  VARCHAR(50) DEFAULT NULL,
  `user_answer`    TEXT,
  `correct_answer` VARCHAR(500) DEFAULT NULL,
  `is_correct`     TINYINT  DEFAULT NULL,
  `explanation`    TEXT,
  `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`        TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  INDEX `idx_student` (`student_id`),
  INDEX `idx_resource` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测验作答表';

CREATE TABLE IF NOT EXISTS `report` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `student_id`   BIGINT   DEFAULT NULL,
  `title`        VARCHAR(200) DEFAULT NULL,
  `content`      TEXT,
  `period_start` DATE     DEFAULT NULL,
  `period_end`   DATE     DEFAULT NULL,
  `metrics`      TEXT,
  `create_time`  DATETIME DEFAULT NULL,
  `deleted`      TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  INDEX `idx_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习报告表';

CREATE TABLE IF NOT EXISTS `conversation` (
  `id`                 BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `student_id`         BIGINT      NOT NULL,
  `session_id`         VARCHAR(100) DEFAULT NULL,
  `question`           TEXT        NOT NULL,
  `answer`             MEDIUMTEXT,
  `intent`             VARCHAR(50) DEFAULT NULL,
  `intent_confidence`  VARCHAR(20) DEFAULT NULL,
  `evaluation_report`  TEXT,
  `resource_dir`       VARCHAR(500) DEFAULT NULL,
  `create_time`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`            TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  INDEX `idx_student` (`student_id`),
  INDEX `idx_session` (`student_id`, `session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话历史表';
