# Java 实例 – 文件写入

**题目描述**: 以下实例演示了使用 write() 方法向文件写入内容： 创建成功后当前目录下就会生成一个名为 runoob.txt 的文件并将 "菜鸟教程" 字符串写入该文件。

```java

/*
 author by runoob.com 
 Main.java
 */
import java.io.*;
 
public class Main {
public static void main(String[] args) {
try {
BufferedWriter out = new BufferedWriter(new FileWriter("runoob.txt"));
            out.write("菜鸟教程");
            out.close();
            System.out.println("文件创建成功！");
        } catch (IOException e) {
}
}
}

```

[原文链接](https://www.runoob.com/java/file-write.html)
