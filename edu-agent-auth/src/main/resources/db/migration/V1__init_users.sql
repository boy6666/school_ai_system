-- auth 服务建表（Flyway 版本化迁移，仅首次执行一次，之后由 flyway_schema_history 记录）。
-- 库（auth_db）由部署编排预建，本脚本只建表、不建库。
-- IF NOT EXISTS 仅用于兼容本地已手动执行过建表 SQL 的旧库；生产全新库会正常建表。

-- 用户表：字段对齐旧单体 users 表（含引导标记 onboarded），角色为单列 role。
CREATE TABLE IF NOT EXISTS `users` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username`         VARCHAR(64)  NOT NULL COMMENT '登录用户名',
    `password`         VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
    `nickname`         VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `email`            VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`            VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar`           VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    `role`             VARCHAR(32)  DEFAULT 'student' COMMENT '角色：student/teacher/admin',
    `status`           VARCHAR(20)  DEFAULT 'active' COMMENT '状态：active/inactive',
    `onboarded`        TINYINT      DEFAULT 0 COMMENT '引导完成标记：0=未完成，1=已完成',
    `last_login_time`  DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`    VARCHAR(64)  DEFAULT NULL COMMENT '最后登录 IP',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';
