-- EduAgent 数据库初始化脚本
-- 创建数据库和基础配置

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `edu_agent` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `edu_agent`;

-- 设置时区
SET time_zone = '+8:00';

-- 显示创建结果
SELECT 'Database edu_agent created successfully!' AS status;