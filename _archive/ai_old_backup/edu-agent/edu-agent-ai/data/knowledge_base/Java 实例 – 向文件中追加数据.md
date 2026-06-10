# Java 实例 – 向文件中追加数据

**题目描述**: 以下实例演示了使用 filewriter 方法向文件中追加数据：

```java

import java.io.*;
 
public class Main {
public static void main(String[] args) throws Exception {
try {
BufferedWriter out = new BufferedWriter(new FileWriter("filename"));
            out.write("aString1\n");
            out.close();
            out = new BufferedWriter(new FileWriter("filename",true));
            out.write("aString2");
            out.close();
            BufferedReader in = new BufferedReader(new FileReader("filename"));
            String str;
            while ((str = in.readLine()) != null) {
System.out.println(str);
            }
in.close();
        }
catch (IOException e) {
System.out.println("exception occoured"+ e);
        }
}
}

```

[原文链接](https://www.runoob.com/java/file-append.html)
