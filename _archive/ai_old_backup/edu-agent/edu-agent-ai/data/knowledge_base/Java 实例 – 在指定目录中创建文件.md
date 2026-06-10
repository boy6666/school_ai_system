# Java 实例 – 在指定目录中创建文件

**题目描述**: 以下实例演示了使用 File 类的  file.createTempFile() 方法在指定目录中创建文件：

```java

import java.io.File;
 
public class Main {
public static void main(String[] args) throws Exception {
File file = null;
        File dir = new File("C:/");
        file = File.createTempFile
("JavaTemp", ".javatemp", dir);
        System.out.println(file.getPath());
    }
}

```

[原文链接](https://www.runoob.com/java/file-dir.html)
