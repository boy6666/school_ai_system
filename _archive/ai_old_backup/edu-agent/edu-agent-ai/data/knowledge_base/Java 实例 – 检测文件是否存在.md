# Java 实例 – 检测文件是否存在

**题目描述**: 以下实例演示了使用 File 类的 file.exists() 方法来检测文件是否存在：

```java

import java.io.File;
 
public class Main {
public static void main(String[] args) {
File file = new File("C:/java.txt");
        System.out.println(file.exists());
    }
}

```

[原文链接](https://www.runoob.com/java/file-exist.html)
