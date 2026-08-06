-- resource 服务建表（Flyway 版本化迁移，仅首次执行一次，之后由 flyway_schema_history 记录）
-- 库由 database/init-microservice.sql（或部署编排）预建，本脚本只建表，不建库。
-- DB-per-service：本服务独占 resource_db，禁止任何服务直连本库；跨服务读一律走 Feign。
-- 去除所有跨库 FK（逻辑引用纯 BIGINT）；deleted 为逻辑删除列（BaseEntity 硬约束，不可缺失）。

CREATE TABLE IF NOT EXISTS `learning_resources` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id`       BIGINT       NOT NULL                COMMENT '归属学生(原 student_id)',
  `title`         VARCHAR(200) NOT NULL                COMMENT '资源标题',
  `type`          VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT 'mindmap|quiz|reading|code|learning_path|review|summary',
  `difficulty`    VARCHAR(20)  NOT NULL DEFAULT 'medium' COMMENT 'easy|medium|hard',
  `chapter`       VARCHAR(100) DEFAULT NULL            COMMENT '章节展示名(原 chapter_name)',
  `chapter_id`    VARCHAR(50)  DEFAULT NULL            COMMENT '章节ID(原 course_id)',
  `course_name`   VARCHAR(100) DEFAULT NULL            COMMENT '课程名',
  `description`   TEXT                               COMMENT '描述',
  `content`       LONGTEXT                           COMMENT 'AI 生成内容(JSON/Markdown 文本)',
  `prompt`        TEXT                               COMMENT '触发本次生成的 prompt(复现/审计用)',
  `ai_task_id`    VARCHAR(64)  DEFAULT NULL            COMMENT '关联 MQ 任务 id(异步去重/追踪)',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'generating' COMMENT 'generating|published|failed|archived',
  `error_msg`     VARCHAR(500) DEFAULT NULL            COMMENT '生成失败原因(降级展示)',
  `rating`        DECIMAL(3,2) DEFAULT 0.00            COMMENT '评分 0-5',
  `views`         INT          DEFAULT 0,
  `favorites`     INT          DEFAULT 0,
  `tags`          JSON                               COMMENT '标签数组',
  `teacher_id`    BIGINT       DEFAULT NULL            COMMENT '教师发布者(预留 P3)',
  `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除 1=已删 0=未删',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_type` (`type`),
  INDEX `idx_status` (`status`),
  INDEX `idx_chapter` (`chapter_id`, `type`),
  INDEX `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习资源表';

CREATE TABLE IF NOT EXISTS `resource_favorites` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id`     BIGINT      NOT NULL,
  `resource_id` BIGINT      NOT NULL,
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP,
  `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  UNIQUE KEY `uk_user_resource` (`user_id`, `resource_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_resource_id` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源收藏表';

CREATE TABLE IF NOT EXISTS `resource_feedback` (
  `id`                   BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id`              BIGINT      NOT NULL,
  `resource_id`          BIGINT      NOT NULL,
  `liked`                TINYINT(1)  DEFAULT NULL,
  `difficulty_feedback`  VARCHAR(50) DEFAULT NULL,
  `create_time`          DATETIME    DEFAULT CURRENT_TIMESTAMP,
  `deleted`              TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  INDEX `idx_resource` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资源反馈表';

-- RAG 清洗后语料：本服务维护清洗流水线产出，ai-service 只经 Feign 拉取/回调，不直连本库。
CREATE TABLE IF NOT EXISTS `kb_corpus` (
  `id`           BIGINT       PRIMARY KEY AUTO_INCREMENT,
  `source`       VARCHAR(100) NOT NULL COMMENT '来源：java_notes/教材/官方文档',
  `chapter`      VARCHAR(100),
  `section`      VARCHAR(100),
  `version`      VARCHAR(32)  DEFAULT 'v1',
  `content`      LONGTEXT     NOT NULL COMMENT '清洗后纯文本（一个 chunk）',
  `content_hash` CHAR(64)     COMMENT 'SHA-256 精确去重',
  `token_count`  INT,
  `status`       TINYINT      DEFAULT 0 COMMENT '0 待向量化 1 已向量化',
  `created_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 1=已删 0=未删',
  UNIQUE KEY `uk_hash` (`content_hash`),
  INDEX `idx_source_chapter` (`source`, `chapter`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG 清洗后语料';
