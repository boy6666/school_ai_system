-- code 服务建表（Flyway 版本化迁移，仅首次执行一次，之后由 flyway_schema_history 记录）
-- IF NOT EXISTS 仅用于兼容本地已手动执行过 schema.sql 的旧库；生产全新库会正常建表。

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
