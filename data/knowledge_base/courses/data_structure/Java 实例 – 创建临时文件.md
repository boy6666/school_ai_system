# Java 实例 – 创建临时文件

**题目描述**: 以下实例演示了使用 File 类的 createTempFile(String prefix, String suffix); 方法在默认临时目录来创建临时文件，参数 prefix  为前缀，suffix 为后缀： 也可以使用createTempFile(String prefix, String suffix, File directory)中的  directory 参数来指定临时文件的目录：

```java

import java.io.*;
 
public class Main {
public static void main(String[] args) throws Exception {
File temp = File.createTempFile("testrunoobtmp", ".txt");
        System.out.println("文件路径: "+temp.getAbsolutePath());
        temp.deleteOnExit();
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write("aString");
        System.out.println("临时文件已创建:");
        out.close();
    }
}

```

[原文链接](https://www.runoob.com/java/file-create-temp.html)
