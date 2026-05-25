# Java 实例 – 将文件内容复制到另一个文件

**题目描述**: 以下实例演示了使用 BufferedWriter 类的 read 和 write 方法将文件内容复制到另一个文件：

```java

import java.io.*;
 
public class Main {
public static void main(String[] args) throws Exception {
BufferedWriter out1 = new BufferedWriter(new FileWriter("srcfile"));
        out1.write("string to be copied\n");
        out1.close();
        InputStream in = new FileInputStream(new File("srcfile"));
        OutputStream out = new FileOutputStream
(new File("destnfile"));
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
out.write(buf, 0, len);
        }
in.close();
        out.close();
        BufferedReader in1 = new BufferedReader(new FileReader("destnfile"));
        String str;
        while ((str = in1.readLine()) != null) {
System.out.println(str);
        }
in1.close();
    }
}

```

[原文链接](https://www.runoob.com/java/file-copy.html)
