# Java 实例 – 查看主机指定文件的最后修改时间

**题目描述**: 以下实例演示了如何查看主机指定文件的最后修改时间：

```java

import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.text.SimpleDateFormat;
 
public class Main {
public static void main(String[] argv) throws Exception {
URL u = new URL("http://127.0.0.1/test/test.html");
        URLConnection uc = u.openConnection();
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd hh:mm:ss");
        uc.setUseCaches(false);
        long timestamp = uc.getLastModified();
        System.out.println("test.html 文件最后修改时间 :" + ft.format(new Date(timestamp)));
    }
}

```

[原文链接](https://www.runoob.com/java/net-filetime.html)
