# 递归 个性化课程讲解文档

## 一、学习目标

本节目标是帮助你理解 `递归` 的核心概念、适用场景、常见错误和代码实现方式。

## 二、结合你的画像

- 当前课程：Java 数据结构
- 基础水平：待评估
- 薄弱点：递归
- 偏好资源：讲解文档、思维导图、练习题、代码案例

## 三、知识点讲解

[资料 1] Java 实例 – 递归创建目录｜来源：E:\college_information\edu-agent\edu-agent-ai\data\knowledge_base\Java 实例 – 递归创建目录.md
# Java 实例 – 递归创建目录

**题目描述**: 以下实例演示了使用 File 类的 mkdirs() 实现递归创建目录 ：

```java

import java.io.File;
 
public class Main {
public static void main(String[] args) {
String directories = "D:\\a\\b\\c\\d\\e\\f\\g\\h\\i";
        File file = new File(directories);
        boolean result = file.mkdirs();
        System.out.println("Status = " + result);
    }
}

```

[原文链接](https://www.runoob.com/java/dir-create.html)



[资料 2] Java 实例 – 阶乘｜来源：E:\college_information\edu-agent\edu-agent-ai\data\knowledge_base\Java 实例 – 阶乘.md
# Java 实例 – 阶乘

**题目描述**: 一个正整数的阶乘（英语：factorial）是所有小于及等于该数的正整数的积，并且有0的阶乘为1。自然数n的阶乘写作n!。 亦即n!=1×2×3×...×n。阶乘亦可以递归方式定义：0!=1，n!=(n-1)!×n。 以下实例演示了 Java 阶乘代码的实现：

```java

public class MainClass {
public static void main(String args[]) {
for (int counter = 0; counter <= 10; counter++){
System.out.printf("%d! = %d\n", counter,
        factorial(counter));
    }
}
publ

## 四、易错点

1. 没有明确基本情况或边界条件。
2. 只会背模板，不理解执行过程。
3. 无法把题目拆成子问题。
4. 代码实现时忽略空值或极端输入。

## 五、学习建议

先用图解理解过程，再看代码案例，最后通过练习题巩固。