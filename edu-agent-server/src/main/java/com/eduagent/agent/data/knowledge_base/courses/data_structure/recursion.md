\---
id: data_structure_recursion
title: 递归基础
course: 数据结构
chapter: 递归思想
topic: 递归
level: beginner
tags:
  \- 数据结构
  \- 递归
  \- 函数调用
  \- 边界条件
prerequisites:
  \- Python 函数
suitable_for:
  \- 代码案例偏好
  \- 易错点强化
updated_at: 2026-04-27
\---

\# 递归基础

\## 学习目标

学生应该理解递归函数的两个核心部分：递归终止条件和递归递推关系。

\## 核心概念

递归是函数直接或间接调用自身的一种方法。递归通常用于解决可以分解为相似子问题的问题。

一个递归函数必须包含：

\1. 终止条件
\2. 递归调用
\3. 子问题规模缩小

\## 易错点

\1. 忘记写终止条件，导致无限递归。
\2. 子问题没有变小，导致递归无法结束。
\3. 不清楚递归返回值如何组合。
\4. 对调用栈理解不足。

\## 示例讲解

计算阶乘：

\```python
def factorial(n):
    if n == 0:
        return 1
    return n * factorial(n - 1)