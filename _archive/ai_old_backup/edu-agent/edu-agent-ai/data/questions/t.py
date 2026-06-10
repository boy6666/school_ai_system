#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import re
import os
import mysql.connector
from mysql.connector import Error

# ==================== 配置 ====================
DB_CONFIG = {
  'host': 'localhost',
  'user': 'root',
  'password': '1234',  # 修改为实际密码
  'database': 'edu_agent',
  'charset': 'utf8mb4'
}

INPUT_FILE = "all_questions.md"

# 分类关键词（与之前保持一致）
CATEGORY_KEYWORDS = {
  "01_Java历史背景与特点": ["历史", "背景", "发展", "jvm", "跨平台", "特点", "简介"],
  "02_基本数据类型与运算符": ["数据类型", "基本类型", "变量", "运算符", "表达式", "int", "float", "double", "char",
                              "boolean", "byte", "final", "常量", "标识符", "instanceof"],
  "03_控制结构": ["循环", "for", "while", "do-while", "条件", "if else", "switch", "break", "continue", "三目"],
  "04_方法": ["方法", "函数", "重载", "overload", "返回值", "参数"],
  "05_数组": ["数组", "array", "下标", "索引"],
  "06_字符串": ["string", "字符串", "stringbuffer", "stringbuilder", "字符"],
  "07_类与对象": ["类", "对象", "class", "object", "new", "构造方法", "static", "成员变量", "private", "public",
                  "protected", "this"],
  "08_继承": ["继承", "extends", "super", "重写", "override"],
  "09_多态": ["多态", "polymorphism", "向上转型", "向下转型"],
  "10_抽象类与接口": ["抽象类", "abstract", "接口", "interface", "implements"],
  "11_GUI入门": ["swing", "gui", "jframe"],
  "12_事件处理": ["事件", "event", "listener"],
  "13_多线程与异常": ["线程", "thread", "runnable", "synchronized", "异常", "exception", "try", "catch", "finally"],
  "14_文件与IO": ["io", "文件", "file", "stream", "流", "reader", "writer", "serializable"],
}
EXTRA_CATEGORY = "00_补充知识点"
EXTRA_KEYWORDS = ["集合", "list", "map", "set", "jdbc", "sql", "数据库", "resultset"]
ALL_CATEGORIES = {**CATEGORY_KEYWORDS, EXTRA_CATEGORY: EXTRA_KEYWORDS}


def classify_question(text):
  text_lower = text.lower()
  best_cat = EXTRA_CATEGORY
  best_score = 0
  for cat, keywords in ALL_CATEGORIES.items():
    score = sum(1 for kw in keywords if kw.lower() in text_lower)
    if score > best_score:
      best_score = score
      best_cat = cat
  return best_cat


def parse_questions_from_md(file_path):
  with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

  # 分割题目：匹配 `## Q001` 或 ## Q001 格式（注意可能存在的反引号）
  # 使用更宽松的模式：查找以 `## Q` 或 ## Q 开头的行，直到下一个类似标记或文件结束
  # 先按行分割，再逐段组合
  lines = content.split('\n')
  questions = []
  current_q = None
  current_lines = []

  for line in lines:
    # 匹配题目标记：可能的形式：`## Q001` 或 ## Q001 或 `## Q001` 后面跟空格和题干
    if re.match(r'^(`)?## Q(\d{3})(`)?', line.strip()):
      # 保存上一个题目
      if current_q is not None:
        questions.append(parse_single_question(current_q, ''.join(current_lines)))
      # 开始新题目
      current_q = line.strip()
      current_lines = [line + '\n']
    else:
      if current_q is not None:
        current_lines.append(line + '\n')
  # 最后一个题目
  if current_q is not None:
    questions.append(parse_single_question(current_q, ''.join(current_lines)))

  return questions


def parse_single_question(q_header, full_text):
  # 提取题号
  q_id_match = re.search(r'Q(\d{3})', q_header)
  q_id = f"Q{q_id_match.group(1)}" if q_id_match else "Q000"

  # 提取题干：第一行（去掉题目标记）
  lines = full_text.split('\n')
  stem_line = lines[0].strip()
  # 去掉题目标记，保留题干文字
  stem = re.sub(r'^(`)?## Q\d{3}(`)?\s*', '', stem_line).strip()
  # 如果题干为空，尝试从下一行取（少数情况）
  if not stem and len(lines) > 1:
    stem = lines[1].strip()

  # 提取选项（- A. ... 格式）
  opt_pattern = r'-\s+([A-D])\.\s+(.*?)(?=\n-\s+[A-D]\.|\n<details|$)'
  options = re.findall(opt_pattern, full_text, re.DOTALL)
  opt_dict = {k: v.strip() for k, v in options}

  # 提取答案和解析
  details_match = re.search(r'<details><summary>查看答案</summary>(.*?)</details>', full_text, re.DOTALL)
  answer = ""
  explanation = ""
  if details_match:
    detail = details_match.group(1)
    ans_match = re.search(r'正确答案：([A-D, ]+)', detail)
    if ans_match:
      answer = ans_match.group(1).strip()
    exp_match = re.search(r'<br/>\*\*解析：\*\*(.*?)(?=\n|$)', detail, re.DOTALL)
    if exp_match:
      explanation = exp_match.group(1).strip()

  # 分类
  category = classify_question(stem + " " + " ".join(opt_dict.values()))

  return {
    'question_id': q_id,
    'category': category,
    'stem': stem,
    'options': opt_dict,
    'answer': answer,
    'explanation': explanation
  }


def create_table(conn):
  create_sql = """
    CREATE TABLE IF NOT EXISTS java_quiz (
        id INT AUTO_INCREMENT PRIMARY KEY,
        category VARCHAR(100) NOT NULL,
        question_id VARCHAR(10) NOT NULL,
        question TEXT NOT NULL,
        option_a VARCHAR(500),
        option_b VARCHAR(500),
        option_c VARCHAR(500),
        option_d VARCHAR(500),
        answer VARCHAR(10) NOT NULL,
        explanation TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        INDEX idx_category (category),
        INDEX idx_question_id (question_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    """
  try:
    cursor = conn.cursor()
    cursor.execute(create_sql)
    conn.commit()
    cursor.close()
    print("✅ 表 java_quiz 已准备就绪")
  except Error as e:
    print(f"❌ 建表失败: {e}")


def insert_question(conn, q):
  sql = """
    INSERT INTO java_quiz 
    (category, question_id, question, option_a, option_b, option_c, option_d, answer, explanation)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
  try:
    cursor = conn.cursor()
    cursor.execute(sql, (
      q['category'],
      q['question_id'],
      q['stem'],
      q['options'].get('A', ''),
      q['options'].get('B', ''),
      q['options'].get('C', ''),
      q['options'].get('D', ''),
      q['answer'],
      q['explanation']
    ))
    conn.commit()
    cursor.close()
    return True
  except Error as e:
    print(f"❌ 插入失败 {q['question_id']}: {e}")
    return False


def main():
  if not os.path.exists(INPUT_FILE):
    print(f"错误：找不到文件 {INPUT_FILE}")
    return

  questions = parse_questions_from_md(INPUT_FILE)
  print(f"共解析到 {len(questions)} 道题目")

  try:
    conn = mysql.connector.connect(**DB_CONFIG)
  except Error as e:
    print(f"❌ 数据库连接失败: {e}")
    return

  create_table(conn)

  # 可选：清空表（根据需要开启）
  # cursor = conn.cursor()
  # cursor.execute("TRUNCATE TABLE java_quiz")
  # conn.commit()
  # cursor.close()
  # print("⚠️ 已清空表原有数据")

  total = 0
  for q in questions:
    if insert_question(conn, q):
      total += 1
      print(f"✅ 已导入 [{total}]: {q['question_id']} -> {q['category']}")
    else:
      print(f"❌ 导入失败: {q['question_id']}")

  conn.close()
  print(f"\n🎉 导入完成！共导入 {total} 条记录到 edu_agent.java_quiz 表。")


if __name__ == "__main__":
  main()
