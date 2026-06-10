# 递归 个性化课程讲解文档

## 一、学习目标

本节目标是帮助你理解 `递归` 的核心概念、适用场景、常见错误和代码实现方式。

## 二、结合你的画像

- 当前课程：Java 数据结构
- 基础水平：基础中等
- 薄弱点：递归、二叉树、链表、图
- 偏好资源：讲解文档、思维导图、练习题、代码案例、图解说明

## 三、知识点讲解

[资料 1] Java 实例 – 删除链表中的元素｜来源：E:\college_information\edu-agent\edu-agent-ai\data\knowledge_base\Java 实例 – 删除链表中的元素.md
# Java 实例 – 删除链表中的元素

**题目描述**: 以下实例演示了使用 clear() 方法来删除链表中的元素：

```java

import java.util.*;
 
public class Main {
public static void main(String[] args) {
LinkedList<String> lList = new LinkedList<String>();
      lList.add("1");
      lList.add("8");
      lList.add("6");
      lList.add("4");
      lList.add("5");
      System.out.println(lList);
      lList.subList(2, 4).clear();
      System.out.println(lList);
   }
}

```

[原文链接](https://www.runoob.com/java/data-replace.html)



[资料 2] Java 实例 – 在链表（LinkedList）的开头和结尾添加元素｜来源：E:\college_information\edu-agent\edu-agent-ai\data\knowledge_base\Java 实例 – 在链表（LinkedList）的开头和结尾添加元素.md
# Java 实例 – 在链表（LinkedList）的开头和结尾添加元素

**题目描述**: 以下实例演示了如何使用 LinkedList 类的 addFirst() 和 addLast() 方法在链表的开头和结尾添加元素：

```java

import java.util.LinkedList;
 
public class Main {
public static void main(String[] args) {
LinkedList<String> lList 

## 四、易错点

1. 没有明确基本情况或边界条件。
2. 只会背模板，不理解执行过程。
3. 无法把题目拆成子问题。
4. 代码实现时忽略空值或极端输入。

## 五、学习建议

先用图解理解过程，再看代码案例，最后通过练习题巩固。