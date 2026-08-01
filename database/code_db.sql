-- ============================================================
-- code 服务（edu-agent-code）表结构 —— code_db
-- 与 edu-agent-code/src/main/resources/schema.sql 保持一致（后者供 Spring 启动时自动建表）。
-- 幂等，可重复执行。
-- ============================================================

CREATE TABLE IF NOT EXISTS `code_exercises` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `title`       VARCHAR(128) NOT NULL,
    `description` TEXT,
    `difficulty`  VARCHAR(16)  DEFAULT 'EASY',
    `language`    VARCHAR(32)  DEFAULT 'java',
    `status`      TINYINT      DEFAULT 1,
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码练习';
