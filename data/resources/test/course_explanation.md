【Java编程个性化讲解文档】

一、学习主题
本次重点学习：字符串

二、学生情况
- 已有基础：基础情况未知，需要从核心概念开始学习
- 薄弱点：字符串
- 认知风格：偏好结构化讲解和代码案例
- 资源偏好：讲解文档、思维导图、练习题、代码案例

三、学习建议
1. 先梳理 字符串 的核心定义和使用场景。
2. 再结合知识库中的示例理解常见错误。
3. 最后通过练习题和代码案例进行巩固。

四、知识库参考
来源：data\knowledge_base\courses\data_structure\java_knowagle_qwq.md
标题：字符串
课程：Java编程
主题：字符串
相关度：35.0
标签：['Java', '字符串', 'String']

内容：
---
id: java_018
title: 字符串
course: Java编程
chapter: 数据结构基础
topic: 字符串
level: beginner
tags:
  - Java
  - 字符串
  - String
prerequisites:
  - 二维数组
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 字符串

## 学习目标

学完本节后，学生应该能够：

1. 理解String的不可变性。
2. 掌握字符串的创建方式。
3. 了解字符串常量池。

## 核心概念

String是不可变类，一旦创建内容不能修改。字符串常量池可复用相同内容的字符串。

## 易错点

1. 用==比较字符串内容。
2. 忘记String的不可变性导致性能问题。
3. 混淆String和char。

## 示例讲解

```java
String s = "hello";
String s2 = new String("hello");
s.equals(s2); // true
s == s2; // false
String s1 = "hello";
String s2 = "hello"; // 复用常量池
String s3 = 