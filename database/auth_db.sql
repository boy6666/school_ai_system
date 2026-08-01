-- ============================================================
-- auth 服务（edu-agent-auth）表结构 —— auth_db
-- 由 database/init-microservice.sql 创建库后，本文件建表。
-- 与 edu-agent-auth/src/main/resources/schema.sql 保持一致（后者供 Spring 启动时自动建表）。
-- 幂等，可重复执行。
-- ============================================================

CREATE TABLE IF NOT EXISTS `users` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username`   VARCHAR(64)  NOT NULL,
    `password`   VARCHAR(100) NOT NULL,
    `real_name`  VARCHAR(64),
    `email`      VARCHAR(100),
    `phone`      VARCHAR(20),
    `status`     TINYINT      DEFAULT 1,
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE IF NOT EXISTS `roles` (
    `id`    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `code`  VARCHAR(32)  NOT NULL,
    `name`  VARCHAR(32)  NOT NULL,
    UNIQUE KEY `uk_roles_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE IF NOT EXISTS `role_user` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    CONSTRAINT `fk_ru_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ru_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

-- 内置角色种子数据（与 DataInitializer 一致；手动初始化时执行）
INSERT IGNORE INTO `roles` (`code`, `name`) VALUES
    ('ROLE_STUDENT', '学生'),
    ('ROLE_TEACHER', '教师'),
    ('ROLE_ADMIN', '管理员');
