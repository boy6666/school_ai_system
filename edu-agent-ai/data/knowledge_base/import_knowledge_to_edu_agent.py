#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import mysql.connector
from mysql.connector import Error

# ==================== 配置区域 ====================
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '1234',    # 修改为你的 MySQL 密码
    'database': 'edu_agent',        # 使用已存在的数据库
    'charset': 'utf8mb4'
}

NOTES_DIR = "knowledge_point"       # 爬虫输出的目录（与脚本同目录或绝对路径）

# 建表 SQL（如果表不存在则创建）
CREATE_TABLE_SQL = """
CREATE TABLE IF NOT EXISTS java_notes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
"""

# 插入 SQL（如果希望避免重复，可改用 INSERT IGNORE 或 ON DUPLICATE KEY UPDATE）
INSERT_SQL = """
INSERT INTO java_notes (category, title, filename, content)
VALUES (%s, %s, %s, %s)
"""
# =================================================

def get_connection():
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        return conn
    except Error as e:
        print(f"❌ 数据库连接失败: {e}")
        return None

def create_table_if_not_exists(conn):
    try:
        cursor = conn.cursor()
        cursor.execute(CREATE_TABLE_SQL)
        conn.commit()
        cursor.close()
        print("✅ 表 java_notes 已准备就绪 (在 edu_agent 数据库中)")
    except Error as e:
        print(f"❌ 建表失败: {e}")

def insert_note(conn, category, title, filename, content):
    try:
        cursor = conn.cursor()
        cursor.execute(INSERT_SQL, (category, title, filename, content))
        conn.commit()
        cursor.close()
        return True
    except Error as e:
        print(f"❌ 插入失败 {category}/{filename}: {e}")
        return False

def main():
    conn = get_connection()
    if not conn:
        return
    create_table_if_not_exists(conn)

    if not os.path.isdir(NOTES_DIR):
        print(f"❌ 目录不存在: {NOTES_DIR}")
        conn.close()
        return

    total = 0
    for root, dirs, files in os.walk(NOTES_DIR):
        category = os.path.basename(root)      # 如 "01_Java历史背景与特点"
        for file in files:
            if not file.endswith('.md'):
                continue
            file_path = os.path.join(root, file)
            title = file[:-3]                  # 去掉 .md 后缀
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
            except Exception as e:
                print(f"⚠️ 读取文件失败 {file_path}: {e}")
                continue

            if insert_note(conn, category, title, file, content):
                total += 1
                print(f"✅ 已导入 [{total}]: {category}/{file}")
            else:
                print(f"❌ 导入失败: {category}/{file}")

    conn.close()
    print(f"\n🎉 导入完成！共导入 {total} 条记录到 edu_agent.java_notes 表。")

if __name__ == "__main__":
    main()
