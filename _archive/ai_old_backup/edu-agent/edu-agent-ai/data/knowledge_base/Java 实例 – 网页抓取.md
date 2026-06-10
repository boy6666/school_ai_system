# Java 实例 – 网页抓取

**题目描述**: 以下实例演示了如何使用 net.URL 类的 URL() 构造函数来抓取网页：

```java

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
 
public class Main {
public static void main(String[] args)
throws Exception {
URL url = new URL("http://www.runoob.com");
      BufferedReader reader = new BufferedReader
(new InputStreamReader(url.openStream()));
      BufferedWriter writer = new BufferedWriter
(new FileWriter("data.html"));
      String line;
      while ((line = reader.readLine()) != null) {
System.out.println(line);
         writer.write(line);
         writer.newLine();
      }
reader.close();
      writer.close();
   }
}

```

[原文链接](https://www.runoob.com/java/net-webpage.html)
