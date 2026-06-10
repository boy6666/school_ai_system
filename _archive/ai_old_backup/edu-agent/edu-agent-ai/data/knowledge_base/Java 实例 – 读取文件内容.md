# Java 实例 – 读取文件内容

**题目描述**: 以下实例演示了使用 readLine() 方法来读取文件 test.log 内容，其中 test.log 文件内容为：

```java

import java.io.*;
 
public class Main {
public static void main(String[] args) {
try {
BufferedReader in = new BufferedReader(new FileReader("test.log"));
            String str;
            while ((str = in.readLine()) != null) {
System.out.println(str);
            }
System.out.println(str);
        } catch (IOException e) {
}
}
}

```

[原文链接](https://www.runoob.com/java/file-read.html)
