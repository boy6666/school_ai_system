# Java 实例 – 获取文件大小

**题目描述**: 以下实例演示了使用 File 类的 file.exists() 和 file.length()  方法来获取文件大小，以字节计算（1KB=1024字节 ）：

```java

import java.io.File;
 
public class Main {
public static long getFileSize(String filename) {
File file = new File(filename);
        if (!file.exists() || !file.isFile()) {
System.out.println("文件不存在");
            return -1;
        }
return file.length();
    }
public static void main(String[] args) {
long size = getFileSize("c:/java.txt");
        System.out.println("java.txt文件大小为: " + size);
    }
}

```

[原文链接](https://www.runoob.com/java/file-size.html)
