# Java 实例 – 获取本机ip地址及主机名

**题目描述**: 在 Java 中，可以使用标准的网络库来获取本机的 IP 地址和主机名。 以下实例演示了如何使用 InetAddress 类的 getLocalHost 和 getLocalAddress() 方法获取本机主机名及 ip 地址：

```java

import java.net.InetAddress;
import java.net.UnknownHostException;
 
public class NetworkInfo {
public static void main(String[] args) {
try {
// 获取本地主机对象
InetAddress localHost = InetAddress.getLocalHost();
            
            // 获取主机名
String hostName = localHost.getHostName();
            System.out.println("主机名: " + hostName);
            
            // 获取IP地址
String hostAddress = localHost.getHostAddress();
            System.out.println("IP地址: " + hostAddress);
        } catch (UnknownHostException e) {
System.err.println("无法获取本机IP地址及主机名: " + e.getMessage());
            e.printStackTrace();
        }
}
}

```

[原文链接](https://www.runoob.com/java/net-localip.html)
