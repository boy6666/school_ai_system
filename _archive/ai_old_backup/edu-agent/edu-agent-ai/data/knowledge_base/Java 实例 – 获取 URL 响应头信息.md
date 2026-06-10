# Java 实例 – 获取 URL 响应头信息

**题目描述**: 以下实例演示了如何获取指定 URL 的响应头信息：

```java

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Set;
 
public class Main {
public static void main(String[] args) throws IOException{
URL url = new URL("http://www.runoob.com");
        URLConnection conn = url.openConnection();
        
        Map headers = conn.getHeaderFields();
        Set<String> keys = headers.keySet();
        for( String key : keys ){
String val = conn.getHeaderField(key);
            System.out.println(key+" "+val);
        }
System.out.println( conn.getLastModified() );
    }
}

```

[原文链接](https://www.runoob.com/java/net-url-header.html)
