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
