# Java 实例 – 获取 URL响应头的日期信息

**题目描述**: 以下实例演示了如何使用 HttpURLConnection 的 httpCon.getDate() 方法来获取 URL响应头的日期信息：

```java

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
 
public class Main{
public static void main(String args[])
throws Exception {
URL url = new URL("http://www.runoob.com");
      HttpURLConnection httpCon = 
      (HttpURLConnection) url.openConnection();
      long date = httpCon.getDate();
      if (date == 0)
System.out.println("无法获取信息。");
      else
System.out.println("Date: " + new Date(date));
   }
}

```

[原文链接](https://www.runoob.com/java/net-urldate.html)
