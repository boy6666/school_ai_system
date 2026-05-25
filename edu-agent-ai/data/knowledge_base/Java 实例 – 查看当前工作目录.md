# Java 实例 – 查看当前工作目录

**题目描述**: 以下实例演示了使用 System 的 getProperty() 方法来获取当前的工作目录：

```java

class Main {
public static void main(String[] args) {
String curDir = System.getProperty("user.dir");
        System.out.println("你当前的工作目录为 :" + curDir);
    }
}

```

[原文链接](https://www.runoob.com/java/dir-current.html)
