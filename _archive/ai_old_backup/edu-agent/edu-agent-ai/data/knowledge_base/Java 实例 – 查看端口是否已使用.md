# Java 实例 – 查看端口是否已使用

**题目描述**: 以下实例演示了如何检测端口是否已经使用： 也可以指定主机的端口：

```java
import java.net.*;
import java.io.*;
 
public class Main {
public static void main(String[] args) {
Socket Skt;
      String host = "localhost";
      if (args.length > 0) {
host = args[0];
      }
for (int i = 0; i < 1024; i++) {
try {
System.out.println("查看 "+ i);
            Skt = new Socket(host, i);
            System.out.println("端口 " + i + " 已被使用");
         }
catch (UnknownHostException e) {
System.out.println("Exception occured"+ e);
            break;
         }
catch (IOException e) {
}
}
}
}
```

[原文链接](https://www.runoob.com/java/net-port.html)
