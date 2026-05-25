# Java 实例 – 使用 Socket 连接到指定主机

**题目描述**: 以下实例演示了如何使用 net.Socket 类的 getInetAddress()  方法来连接到指定主机：

```java

import java.net.InetAddress;
import java.net.Socket;
 
public class WebPing {
public static void main(String[] args) {
try {
InetAddress addr;
            Socket sock = new Socket("www.runoob.com", 80);
            addr = sock.getInetAddress();
            System.out.println("连接到 " + addr);
            sock.close();
        } catch (java.io.IOException e) {
System.out.println("无法连接 " + args[0]);
            System.out.println(e);
        }
}
}

```

[原文链接](https://www.runoob.com/java/net-connected.html)
