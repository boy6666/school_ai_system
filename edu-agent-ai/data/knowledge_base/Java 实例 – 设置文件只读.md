# Java 实例 – 设置文件只读

**题目描述**: 以下实例演示了使用 File 类的 file.setReadOnly() 和 file.canWrite() 方法来设置文件只读：

```java

import java.io.File;
 
public class Main {
public static void main(String[] args) {
File file = new File("C:/java.txt");
        System.out.println(file.setReadOnly());
        System.out.println(file.canWrite());
    }
}

```

[原文链接](https://www.runoob.com/java/file-read-only.html)
