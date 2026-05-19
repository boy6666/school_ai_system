---
id: data_structure_tree
title: 树结构基础
course: 数据结构
chapter: 树与二叉树
topic: 树结构
level: beginner
tags:
  - 数据结构
  - 树
  - 二叉树
  - 递归
prerequisites:
  - 递归基础
  - 链表基础
suitable_for:
  - 图解型学习者
  - 代码案例偏好
updated_at: 2026-04-27
---

# 树结构基础

## 学习目标

学完本节后，学生应该能够：

1. 理解树、根节点、叶子节点、父子节点的概念。
2. 区分树的高度、深度、层数。
3. 理解二叉树的基本结构。
4. 能用递归方式遍历二叉树。

## 核心概念

树是一种非线性数据结构，由节点和边组成。树有一个根节点，每个节点可以有若干子节点。

二叉树是一种特殊的树，每个节点最多有两个子节点，通常称为左子树和右子树。

## 易错点

1. 容易混淆高度和深度。
2. 容易忘记递归终止条件。
3. 容易把前序、中序、后序遍历顺序记反。
4. 写代码时容易漏掉空节点判断。

## 示例讲解

以前序遍历为例，遍历顺序是：

根节点 -> 左子树 -> 右子树

如果一棵树是：

A 的左孩子是 B，右孩子是 C

那么前序遍历结果是：

A B C

## 代码示例

```python
def preorder(root):
    if root is None:
        return []

    return [root.val] + preorder(root.left) + preorder(root.right)
---
id: java_001
title: Java程序结构
course: Java编程
chapter: 基础语法
topic: 程序结构
level: beginner
tags:
  - Java
  - 基础语法
  - main方法
prerequisites:
  - 计算机基础
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27


# Java程序结构

## 学习目标

学完本节后，学生应该能够：

1. 理解Java程序的基本组成。
2. 掌握main方法的写法。
3. 能够编写并运行一个简单的Java程序。

## 核心概念

Java程序由类和main方法组成，main方法是程序的入口。一个Java源文件可以包含多个类，但只能有一个public类。

## 易错点

1. 容易忘记main方法的签名是`public static void main(String[] args)`。
2. 忘记文件名必须与public类名一致。
3. 混淆`System.out.print`和`System.out.println`。

## 示例讲解

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}
public class Main {
    public static void main(String[] args) {
        // 程序入口
    }
}
---
id: java_002
title: 变量
course: Java编程
chapter: 基础语法
topic: 变量
level: beginner
tags:
  - Java
  - 变量
  - 数据类型
prerequisites:
  - Java程序结构
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 变量

## 学习目标

学完本节后，学生应该能够：

1. 理解变量的概念和作用。
2. 掌握变量的声明和初始化。
3. 了解变量的命名规范。

## 核心概念

变量用于存储数据，需要声明类型。Java是强类型语言，每个变量必须先声明类型才能使用。

## 易错点

1. 忘记声明变量类型。
2. 变量未初始化就使用。
3. 变量名与关键字冲突。

## 示例讲解

```java
int a = 10;
int a = 10;
String name = "张三";
double price = 19.99;
boolean flag = true;
---
id: java_003
title: 常量
course: Java编程
chapter: 基础语法
topic: 常量
level: beginner
tags:
  - Java
  - 常量
  - final
prerequisites:
  - 变量
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 常量

## 学习目标

学完本节后，学生应该能够：

1. 理解常量的概念。
2. 掌握final关键字的使用。
3. 了解常量的命名规范。

## 核心概念

常量使用final关键字修饰，一旦赋值就不能修改。常量名通常使用全大写字母。

## 易错点

1. 尝试修改final修饰的变量。
2. 常量声明后忘记初始化。
3. 混淆常量和变量的使用场景。

## 示例讲解

```java
final int X = 10;
X = 20; // 编译错误
final int MAX_SIZE = 100;
final double PI = 3.14159;
final String APP_NAME = "MyApp";
---
id: java_004
title: 数据类型
course: Java编程
chapter: 基础语法
topic: 数据类型
level: beginner
tags:
  - Java
  - 数据类型
  - 基本类型
prerequisites:
  - 变量
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 数据类型

## 学习目标

学完本节后，学生应该能够：

1. 掌握Java的8种基本数据类型。
2. 理解每种类型的取值范围。
3. 区分基本类型和引用类型。

## 核心概念

Java数据类型分为基本类型和引用类型。基本类型包括：byte、short、int、long、float、double、char、boolean。

## 易错点

1. float类型赋值时忘记加f后缀。
2. long类型赋值时忘记加L后缀。
3. 混淆char和String的区别。

## 示例讲解

```java
int a = 10;
double d = 1.2;
char c = 'A';
boolean b = true;
byte b = 127;
short s = 32767;
int i = 100;
long l = 100L;
float f = 3.14f;
double d = 3.14159;
char c = 'A';
boolean flag = true;
---
id: java_005
title: 类型转换
course: Java编程
chapter: 基础语法
topic: 类型转换
level: beginner
tags:
  - Java
  - 类型转换
  - 强制转换
prerequisites:
  - 数据类型
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 类型转换

## 学习目标

学完本节后，学生应该能够：

1. 理解自动类型转换的规则。
2. 掌握强制类型转换的语法。
3. 了解类型转换中可能出现的精度丢失问题。

## 核心概念

类型转换分为自动转换和强制转换。小范围类型可以自动转为大范围类型，大范围转小范围需要强制转换。

## 易错点

1. 强制转换可能导致精度丢失。
2. byte和short运算时自动提升为int。
3. char类型参与运算时转为int。

## 示例讲解

```java
int a = (int) 3.5; // 结果为3，小数部分丢失

// 自动转换
int i = 100;
long l = i;

// 强制转换
double d = 3.14;
int j = (int) d;
---
id: java_006
title: 运算符
course: Java编程
chapter: 基础语法
topic: 运算符
level: beginner
tags:
  - Java
  - 运算符
  - 算术运算
prerequisites:
  - 数据类型
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 运算符

## 学习目标

学完本节后，学生应该能够：

1. 掌握算术运算符的使用。
2. 理解取模运算。
3. 了解自增自减运算符。

## 核心概念

算术运算符包括：+（加）、-（减）、*（乘）、/（除）、%（取模）。整数除法结果仍为整数。

## 易错点

1. 整数除以0会抛ArithmeticException异常。
2. 整数除法自动取整，丢失小数。
3. 自增运算符在前在后的区别。

## 示例讲解

```java
int c = a + b;
int d = 10 / 3; // 结果为3，不是3.33
int a = 10;
int b = 3;
int sum = a + b;
int diff = a - b;
int prod = a * b;
int quot = a / b;
int mod = a % b;
---
id: java_007
title: 关系运算符
course: Java编程
chapter: 基础语法
topic: 关系运算符
level: beginner
tags:
  - Java
  - 关系运算符
  - 比较
prerequisites:
  - 运算符
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 关系运算符

## 学习目标

学完本节后，学生应该能够：

1. 掌握关系运算符的使用。
2. 理解关系运算符的返回值类型。
3. 区分==和=的区别。

## 核心概念

关系运算符用于比较两个值，返回boolean类型结果。包括：==、!=、>、<、>=、<=。

## 易错点

1. 混淆==（比较）和=（赋值）。
2. 浮点数比较不要直接用==。
3. 引用类型==比较的是地址。

## 示例讲解

```java
boolean result = a > 5;
int a = 10;
int b = 20;
boolean eq = a == b;
boolean ne = a != b;
boolean gt = a > b;
boolean lt = a < b;
boolean ge = a >= b;
boolean le = a <= b;
---
id: java_008
title: 逻辑运算符
course: Java编程
chapter: 基础语法
topic: 逻辑运算符
level: beginner
tags:
  - Java
  - 逻辑运算符
  - 短路运算
prerequisites:
  - 关系运算符
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 逻辑运算符

## 学习目标

学完本节后，学生应该能够：

1. 掌握逻辑运算符的使用。
2. 理解短路运算的原理。
3. 区分&和&&的区别。

## 核心概念

逻辑运算符包括：&&（与）、||（或）、!（非）。&&和||具有短路特性。

## 易错点

1. 混淆&和&&的区别。
2. 混淆|和||的区别。
3. 逻辑运算符优先级容易搞错。

## 示例讲解

```java
boolean result = a > 0 && b > 0;
boolean a = true;
boolean b = false;
boolean and = a && b; // false
boolean or = a || b; // true
boolean not = !a; // false
---
id: java_009
title: if语句
course: Java编程
chapter: 流程控制
topic: if语句
level: beginner
tags:
  - Java
  - if语句
  - 条件判断
prerequisites:
  - 逻辑运算符
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# if语句

## 学习目标

学完本节后，学生应该能够：

1. 掌握if语句的基本语法。
2. 理解条件表达式的编写。
3. 能够使用if语句控制程序流程。

## 核心概念

if语句用于根据条件执行不同的代码块。条件表达式必须返回boolean类型。

## 易错点

1. 条件表达式中使用=而不是==。
2. 忘记写大括号导致逻辑错误。
3. 条件表达式写反。

## 示例讲解

```java
if (a > 0) {
    System.out.println("正数");
}
int score = 85;
if (score >= 60) {
    System.out.println("及格");
}
---
id: java_010
title: if-else语句
course: Java编程
chapter: 流程控制
topic: if-else
level: beginner
tags:
  - Java
  - if-else
  - 双分支
prerequisites:
  - if语句
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# if-else语句

## 学习目标

学完本节后，学生应该能够：

1. 掌握if-else的语法。
2. 理解双分支结构的使用场景。
3. 能够使用if-else if-else处理多分支。

## 核心概念

if-else是双分支结构，if条件成立执行一块代码，否则执行另一块。多个条件可使用if-else if-else。

## 易错点

1. 忘记写else后的大括号。
2. 多条件判断时条件顺序不合理。
3. else与哪个if配对搞混。

## 示例讲解

```java
if (a > 0) {
    System.out.println("正数");
} else {
    System.out.println("非正数");
}
int score = 75;
if (score >= 90) {
    System.out.println("优秀");
} else if (score >= 60) {
    System.out.println("及格");
} else {
    System.out.println("不及格");
}
---
id: java_011
title: switch语句
course: Java编程
chapter: 流程控制
topic: switch
level: beginner
tags:
  - Java
  - switch
  - 多分支
prerequisites:
  - if-else
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# switch语句

## 学习目标

学完本节后，学生应该能够：

1. 掌握switch语句的语法。
2. 理解break的作用。
3. 了解switch支持的数据类型。

## 核心概念

switch用于多分支选择，支持byte、short、int、char、String和枚举类型。每个case后需要break防止穿透。

## 易错点

1. 忘记写break导致case穿透。
2. 忘记写default分支。
3. case值重复。

## 示例讲解

```java
switch (x) {
    case 1:
        System.out.println("一");
        break;
    default:
        System.out.println("其他");
}
int day = 3;
switch (day) {
    case 1:
        System.out.println("周一");
        break;
    case 2:
        System.out.println("周二");
        break;
    default:
        System.out.println("其他");
}
---
id: java_012
title: for循环
course: Java编程
chapter: 流程控制
topic: for循环
level: beginner
tags:
  - Java
  - for循环
  - 循环结构
prerequisites:
  - switch
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# for循环

## 学习目标

学完本节后，学生应该能够：

1. 掌握for循环的语法。
2. 理解初始化、条件、迭代三部分。
3. 能够使用for循环解决重复操作。

## 核心概念

for循环是最常用的循环结构，由初始化表达式、条件表达式、迭代表达式三部分组成。

## 易错点

1. 条件表达式写错导致死循环。
2. 三部分之间用分号隔开。
3. 循环变量作用域问题。

## 示例讲解

```java
for (int i = 0; i < 5; i++) {
    System.out.println(i);
}
for (int i = 0; i < 10; i++) {
    System.out.println("第" + i + "次");
}
---
id: java_013
title: while循环
course: Java编程
chapter: 流程控制
topic: while循环
level: beginner
tags:
  - Java
  - while循环
  - 条件循环
prerequisites:
  - for循环
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# while循环

## 学习目标

学完本节后，学生应该能够：

1. 掌握while循环的语法。
2. 理解while和for的使用场景区别。
3. 能够避免while死循环。

## 核心概念

while循环在条件为true时重复执行代码块。适用于循环次数不确定的场景。

## 易错点

1. 忘记更新循环变量导致死循环。
2. 条件表达式写错。
3. while后漏写条件括号。

## 示例讲解

```java
int i = 0;
while (i < 5) {
    System.out.println(i);
    i++;
}
int input;
do {
    System.out.println("请输入选项：");
    input = scanner.nextInt();
} while (input != 0);
---
id: java_015
title: 数组
course: Java编程
chapter: 数据结构基础
topic: 数组
level: beginner
tags:
  - Java
  - 数组
  - 数据结构
prerequisites:
  - do-while
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 数组

## 学习目标

学完本节后，学生应该能够：

1. 理解数组的概念和作用。
2. 掌握数组的声明和初始化。
3. 了解数组的默认值。

## 核心概念

数组用于存储同类型的多个数据，长度固定。数组元素通过索引访问，索引从0开始。

## 易错点

1. 数组索引越界。
2. 数组长度是属性不是方法。
3. 声明和初始化方式混淆。

## 示例讲解

```java
int[] a = new int[5];
a[0] = 1;

int[] arr1 = new int[5];
int[] arr2 = {1, 2, 3, 4, 5};
int[] arr3 = new int[]{1, 2, 3};
---
id: java_016
title: 数组遍历
course: Java编程
chapter: 数据结构基础
topic: 数组遍历
level: beginner
tags:
  - Java
  - 数组
  - 遍历
prerequisites:
  - 数组
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 数组遍历

## 学习目标

学完本节后，学生应该能够：

1. 掌握for循环遍历数组。
2. 掌握增强for循环遍历数组。
3. 理解数组length属性的使用。

## 核心概念

数组遍历是逐个访问数组元素的过程。常用for循环和增强for循环两种方式。

## 易错点

1. 循环条件写成i<=a.length导致越界。
2. 增强for循环中无法获取索引。
3. 增强for循环中不能修改数组元素。

## 示例讲解

```java
for (int i = 0; i < a.length; i++) {
    System.out.println(a[i]);
}
int[] arr = {1, 2, 3, 4, 5};
for (int num : arr) {
    System.out.println(num);
}
---
id: java_017
title: 二维数组
course: Java编程
chapter: 数据结构基础
topic: 二维数组
level: beginner
tags:
  - Java
  - 二维数组
  - 矩阵
prerequisites:
  - 数组遍历
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 二维数组

## 学习目标

学完本节后，学生应该能够：

1. 理解二维数组的概念。
2. 掌握二维数组的声明和初始化。
3. 能够遍历二维数组。

## 核心概念

二维数组是数组的数组，可看作矩阵。每行的列数可以不同（锯齿数组）。

## 易错点

1. 行列索引搞混。
2. 忘记声明第二维大小。
3. length属性获取的是行数。

## 示例讲解

```java
int[][] a = new int[3][3];
a[0][0] = 1;
int[][] matrix = {
    {1, 2, 3},
    {4, 5, 6},
    {7, 8, 9}
};
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
String s3 = new String("hello");
---
id: java_019
title: 字符串方法
course: Java编程
chapter: 数据结构基础
topic: 字符串方法
level: beginner
tags:
  - Java
  - 字符串
  - 方法
prerequisites:
  - 字符串
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 字符串方法

## 学习目标

学完本节后，学生应该能够：

1. 掌握常用字符串方法。
2. 理解equals和==的区别。
3. 能够处理字符串常见操作。

## 核心概念

String类提供丰富的方法：length()、substring()、equals()、indexOf()、charAt()等。

## 易错点

1. substring参数是左闭右开。
2. 用==比较字符串内容。
3. indexOf找不到返回-1。

## 示例讲解

```java
String s = "hello";
s.length(); // 5
s.charAt(0); // 'h'
s.substring(1, 4); // "ell"
String s = "Hello World";
int len = s.length();
String sub = s.substring(0, 5);
int idx = s.indexOf("World");
boolean eq = s.equals("hello");
---
id: java_020
title: StringBuilder
course: Java编程
chapter: 数据结构基础
topic: StringBuilder
level: beginner
tags:
  - Java
  - StringBuilder
  - 字符串拼接
prerequisites:
  - 字符串方法
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# StringBuilder

## 学习目标

学完本节后，学生应该能够：

1. 理解StringBuilder的作用。
2. 掌握StringBuilder常用方法。
3. 知道StringBuilder和StringBuffer的区别。

## 核心概念

StringBuilder是可变的字符串类，用于高效拼接字符串。非线程安全，但性能优于StringBuffer。

## 易错点

1. 用String频繁拼接导致性能问题。
2. 忘记toString()转换回String。
3. StringBuilder和StringBuffer使用场景混淆。

## 示例讲解

```java
StringBuilder sb = new StringBuilder();
sb.append("hello");
sb.append(" world");
String result = sb.toString();
StringBuilder sb = new StringBuilder();
sb.append("Hello");
sb.append(" ");
sb.append("World");
String s = sb.toString();
---
id: java_021
title: 方法
course: Java编程
chapter: 方法与面向对象
topic: 方法
level: beginner
tags:
  - Java
  - 方法
  - 函数
prerequisites:
  - StringBuilder
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 方法

## 学习目标

学完本节后，学生应该能够：

1. 理解方法的概念和作用。
2. 掌握方法的定义和调用。
3. 理解返回值类型和参数。

## 核心概念

方法是封装代码块的机制，提高代码复用性。包含修饰符、返回值类型、方法名、参数列表和方法体。

## 易错点

1. 忘记写return语句。
2. 返回值类型与方法不匹配。
3. 方法调用参数数量或类型错误。

## 示例讲解

```java
int add(int a, int b) {
    return a + b;
}
public static int max(int a, int b) {
    return a > b ? a : b;
}
---
id: java_022
title: 类
course: Java编程
chapter: 方法与面向对象
topic: 类
level: beginner
tags:
  - Java
  - 类
  - 面向对象
prerequisites:
  - 方法
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 类

## 学习目标

学完本节后，学生应该能够：

1. 理解类的概念。
2. 掌握类的定义语法。
3. 了解成员变量和方法。

## 核心概念

类是对象的模板，定义了一类事物的属性和行为。类包含成员变量（属性）和方法（行为）。

## 易错点

1. 类名与文件名不一致。
2. 一个文件多个public类。
3. 成员变量没有初始化。

## 示例讲解

```java
class Person {
    String name;
    int age;
}
public class Student {
    String name;
    int age;
    
    void study() {
        System.out.println(name + "在学习");
    }
}
---
id: java_023
title: 对象
course: Java编程
chapter: 方法与面向对象
topic: 对象
level: beginner
tags:
  - Java
  - 对象
  - 实例化
prerequisites:
  - 类
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 对象

## 学习目标

学完本节后，学生应该能够：

1. 理解对象和类的关系。
2. 掌握new关键字创建对象。
3. 能够通过对象调用方法。

## 核心概念

对象是类的实例，使用new关键字创建。通过对象可以访问其属性和方法。

## 易错点

1. 忘记使用new创建对象。
2. 空指针异常（NPE）。
3. 混淆对象和类的概念。

## 示例讲解

```java
Person p = new Person();
p.name = "张三";
p.sayHello();
Student stu = new Student();
stu.name = "李四";
stu.age = 20;
stu.study();
---
id: java_024
title: 构造方法
course: Java编程
chapter: 方法与面向对象
topic: 构造方法
level: beginner
tags:
  - Java
  - 构造方法
  - 初始化
prerequisites:
  - 对象
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 构造方法

## 学习目标

学完本节后，学生应该能够：

1. 理解构造方法的作用。
2. 掌握构造方法的定义。
3. 了解默认构造方法。

## 核心概念

构造方法用于初始化对象，方法名与类名相同，没有返回值。可重载多个构造方法。

## 易错点

1. 构造方法写返回值。
2. 忘记写构造方法。
3. this调用构造方法必须在第一行。

## 示例讲解

```java
class Person {
    Person() {
        this.name = "未知";
    }
}
public class Student {
    String name;
    
    Student() { }
    
    Student(String name) {
        this.name = name;
    }
}
---
id: java_025
title: 封装
course: Java编程
chapter: 面向对象
topic: 封装
level: intermediate
tags:
  - Java
  - 封装
  - private
prerequisites:
  - 构造方法
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 封装

## 学习目标

学完本节后，学生应该能够：

1. 理解封装的概念和好处。
2. 掌握private关键字的使用。
3. 能够编写getter和setter方法。

## 核心概念

封装是隐藏内部实现细节，通过公共方法访问私有属性。提高安全性和可维护性。

## 易错点

1. 直接访问私有属性。
2. getter/setter命名不规范。
3. 属性都设为public。

## 示例讲解

```java
class Person {
    private int age;
    
    public int getAge() {
        return age;
    }
}
public class BankAccount {
    private double balance;
    
    public double getBalance() {
        return balance;
    }
    
    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }
}
---
id: java_026
title: 继承
course: Java编程
chapter: 面向对象
topic: 继承
level: intermediate
tags:
  - Java
  - 继承
  - extends
prerequisites:
  - 封装
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 继承

## 学习目标

学完本节后，学生应该能够：

1. 理解继承的概念和作用。
2. 掌握extends关键字的使用。
3. 了解super关键字。

## 核心概念

继承允许子类复用父类的属性和方法。Java单继承，一个类只能有一个直接父类。

## 易错点

1. Java不支持多继承。
2. super调用父类构造方法必须在第一行。
3. 私有成员不能被继承。

## 示例讲解

```java
class Animal {
    void eat() { }
}

class Dog extends Animal {
    void bark() { }
}
class Animal {
    String name;
    
    Animal(String name) {
        this.name = name;
    }
    
    void eat() {
        System.out.println(name + "在吃东西");
    }
}

class Dog extends Animal {
    Dog(String name) {
        super(name);
    }
    
    void bark() {
        System.out.println(name + "在叫");
    }
}
---
id: java_027
title: 多态
course: Java编程
chapter: 面向对象
topic: 多态
level: intermediate
tags:
  - Java
  - 多态
  - 动态绑定
prerequisites:
  - 继承
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 多态

## 学习目标

学完本节后，学生应该能够：

1. 理解多态的概念。
2. 掌握向上转型和向下转型。
3. 理解动态绑定。

## 核心概念

多态是同一个行为具有不同表现形式。父类引用指向子类对象，调用方法时执行子类重写的方法。

## 易错点

1. 编译类型和运行类型混淆。
2. 向下转型失败抛出ClassCastException。
3. 静态方法没有多态。

## 示例讲解

```java
Animal a = new Dog();
a.eat(); // 调用Dog的eat方法
Animal a = new Cat();
a.eat(); // 编译看Animal，运行看Cat

if (a instanceof Dog) {
    Dog d = (Dog) a;
    d.bark();
}
---
id: java_028
title: 重写
course: Java编程
chapter: 面向对象
topic: 重写
level: intermediate
tags:
  - Java
  - 重写
  - Override
prerequisites:
  - 多态
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 重写

## 学习目标

学完本节后，学生应该能够：

1. 理解重写的概念。
2. 掌握重写的规则。
3. 能够使用@Override注解。

## 核心概念

重写是子类对父类方法重新实现。方法名、参数列表、返回类型必须相同，访问权限不能更低。

## 易错点

1. 参数不同变成重载不是重写。
2. 访问权限变小。
3. 返回类型不兼容。

## 示例讲解

```java
class Parent {
    void show() { }
}

class Child extends Parent {
    @Override
    void show() { }
}
class Animal {
    void makeSound() {
        System.out.println("动物叫");
    }
}

class Cat extends Animal {
    @Override
    void makeSound() {
        System.out.println("喵喵");
    }
}
---
id: java_029
title: 重载
course: Java编程
chapter: 面向对象
topic: 重载
level: intermediate
tags:
  - Java
  - 重载
  - Overload
prerequisites:
  - 重写
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 重载

## 学习目标

学完本节后，学生应该能够：

1. 理解重载的概念。
2. 掌握重载的规则。
3. 区分重载和重写。

## 核心概念

重载是在同一个类中定义多个同名方法，参数列表必须不同。仅返回值不同不算重载。

## 易错点

1. 仅返回值不同不是重载。
2. 参数类型顺序不同算重载。
3. 混淆重载和重写。

## 示例讲解

```java
int add(int a, int b) { return a + b; }
double add(double a, double b) { return a + b; }
class Calculator {
    int add(int a, int b) {
        return a + b;
    }
    
    int add(int a, int b, int c) {
        return a + b + c;
    }
    
    double add(double a, double b) {
        return a + b;
    }
}

class Calculator {
    int add(int a, int b) {
        return a + b;
    }
    
    int add(int a, int b, int c) {
        return a + b + c;
    }
    
    double add(double a, double b) {
        return a + b;
    }
}
---
id: java_030
title: 抽象类
course: Java编程
chapter: 面向对象
topic: 抽象类
level: intermediate
tags:
  - Java
  - 抽象类
  - abstract
prerequisites:
  - 重载
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 抽象类

## 学习目标

学完本节后，学生应该能够：

1. 理解抽象类的概念。
2. 掌握abstract关键字的使用。
3. 知道抽象类的作用。

## 核心概念

抽象类不能实例化，可以包含抽象方法和具体方法。子类必须实现所有抽象方法。

## 易错点

1. 尝试实例化抽象类。
2. 子类未实现所有抽象方法。
3. abstract和final不能同时使用。

## 示例讲解

```java
abstract class Shape {
    abstract double getArea();
}
abstract class Animal {
    abstract void makeSound();
    
    void sleep() {
        System.out.println("睡觉");
    }
}

class Dog extends Animal {
    @Override
    void makeSound() {
        System.out.println("汪汪");
    }
}
---
id: java_031
title: 接口
course: Java编程
chapter: 面向对象
topic: 接口
level: intermediate
tags:
  - Java
  - 接口
  - interface
prerequisites:
  - 抽象类
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 接口

## 学习目标

学完本节后，学生应该能够：

1. 理解接口的概念。
2. 掌握接口的定义和实现。
3. 了解接口的默认方法和静态方法。

## 核心概念

接口定义行为规范，实现类必须实现所有抽象方法。Java 8+支持默认方法和静态方法。

## 易错点

1. 接口不能实例化。
2. 忘记实现接口方法。
3. 接口多继承的冲突处理。

## 示例讲解

```java
interface Flyable {
    void fly();
}

class Bird implements Flyable {
    public void fly() { }
}
interface Runable {
    void run();
    
    default void warmUp() {
        System.out.println("热身");
    }
}

class Athlete implements Runable {
    @Override
    public void run() {
        System.out.println("跑步");
    }
}
---
id: java_032
title: 异常
course: Java编程
chapter: 高级特性
topic: 异常处理
level: intermediate
tags:
  - Java
  - 异常
  - try-catch
prerequisites:
  - 接口
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 异常

## 学习目标

学完本节后，学生应该能够：

1. 理解异常的概念和分类。
2. 掌握try-catch-finally语法。
3. 能够抛出和自定义异常。

## 核心概念

异常是程序运行时的错误。分为检查异常和非检查异常。使用try-catch处理异常。

## 易错点

1. 异常捕获顺序错误。
2. finally中return会覆盖前面return。
3. 遗漏finally释放资源。

## 示例讲解

```java
try {
    int a = 10 / 0;
} catch (ArithmeticException e) {
    System.out.println("除零错误");
}
try {
    int[] arr = new int[5];
    arr[10] = 1;
} catch (ArrayIndexOutOfBoundsException e) {
    System.out.println("数组越界：" + e.getMessage());
} finally {
    System.out.println("清理资源");
}
---
id: java_033
title: 集合
course: Java编程
chapter: 高级特性
topic: 集合框架
level: intermediate
tags:
  - Java
  - 集合
  - Collection
prerequisites:
  - 异常
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# 集合

## 学习目标

学完本节后，学生应该能够：

1. 理解集合框架的体系结构。
2. 了解Collection和Map接口。
3. 知道集合和数组的区别。

## 核心概念

集合是动态数据结构，可存储和操作一组对象。主要分为Collection和Map两大体系。

## 易错点

1. 集合只能存引用类型。
2. 忘记泛型导致类型不安全。
3. 修改集合时遍历异常。

## 示例讲解

```java
List<String> list = new ArrayList<>();
list.add("hello");

List<Integer> list = new ArrayList<>();
Set<String> set = new HashSet<>();
Map<String, Integer> map = new HashMap<>();
---
id: java_034
title: ArrayList
course: Java编程
chapter: 高级特性
topic: ArrayList
level: intermediate
tags:
  - Java
  - ArrayList
  - 动态数组
prerequisites:
  - 集合
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# ArrayList

## 学习目标

学完本节后，学生应该能够：

1. 理解ArrayList的实现原理。
2. 掌握ArrayList常用方法。
3. 知道ArrayList和数组的区别。

## 核心概念

ArrayList是基于动态数组实现的List，查询快增删慢。线程不安全，初始容量10，扩容1.5倍。

## 易错点

1. 索引越界。
2. 遍历时删除元素导致异常。
3. 忘记指定初始容量。

## 示例讲解

```java
List<String> list = new ArrayList<>();
list.add("A");
list.get(0);
ArrayList<String> list = new ArrayList<>();
list.add("Hello");
list.add("World");
list.remove(0);
String s = list.get(0);
int size = list.size();
boolean isEmpty = list.isEmpty();
---
id: java_035
title: HashMap
course: Java编程
chapter: 高级特性
topic: HashMap
level: intermediate
tags:
  - Java
  - HashMap
  - 哈希表
prerequisites:
  - ArrayList
suitable_for:
  - 代码案例偏好
updated_at: 2026-04-27
---

# HashMap

## 学习目标

学完本节后，学生应该能够：

1. 理解HashMap的实现原理。
2. 掌握HashMap常用方法。
3. 了解HashMap的扩容机制。

## 核心概念

HashMap是基于哈希表实现的Map，键值对存储。JDK8采用数组+链表+红黑树结构。初始容量16，负载因子0.75。

## 易错点

1. key重复会覆盖。
2. null可以作为key。
3. 线程不安全。

## 示例讲解

```java
Map<String, Integer> map = new HashMap<>();
map.put("key", 1);
map.get("key");

HashMap<String, Integer> map = new HashMap<>();
map.put("apple", 1);
map.put("banana", 2);

int value = map.get("apple");
boolean hasKey = map.containsKey("apple");
map.remove("apple");

for (Map.Entry<String, Integer> entry : map.entrySet()) {
    System.out.println(entry.getKey() + "=" + entry.getValue());
}
