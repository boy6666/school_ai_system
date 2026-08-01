-- ============================================================
-- EduAgent 微服务数据库初始化（Linux 联调 / 生产）
-- 由 mysql 容器首次启动（/var/lib/mysql 为空）时自动执行：
--   - 创建 5 个逻辑库（DB-per-service 逻辑隔离，禁止跨库外键）
--   - 创建专用账号 edu_agent 并分别授权
-- 各服务的建表 DDL 由对应开发者在自己的模块迁移脚本里执行，不在本文件。
-- 注意：单体遗留的 database/init.sql（单库 edu_agent）仅作迁移参考，本部署不加载。
-- ============================================================

CREATE DATABASE IF NOT EXISTS auth_db     CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS learning_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS resource_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS teacher_db  CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS code_db     CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

CREATE USER IF NOT EXISTS 'edu_agent'@'%' IDENTIFIED BY 'edu_agent';
GRANT ALL PRIVILEGES ON auth_db.*     TO 'edu_agent'@'%';
GRANT ALL PRIVILEGES ON learning_db.* TO 'edu_agent'@'%';
GRANT ALL PRIVILEGES ON resource_db.* TO 'edu_agent'@'%';
GRANT ALL PRIVILEGES ON teacher_db.*  TO 'edu_agent'@'%';
GRANT ALL PRIVILEGES ON code_db.*     TO 'edu_agent'@'%';
FLUSH PRIVILEGES;
