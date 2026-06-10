# Java 实例 – 获取指定主机的IP地址

**题目描述**: 以下实例演示了如何使用 InetAddress 类的 InetAddress.getByName() 方法来获取指定主机（网址）的IP地址：

```java

import java.net.InetAddress;
import java.net.UnknownHostException;
 
public class GetIP {
public static void main(String[] args) {
InetAddress address = null;
        try {
address = InetAddress.getByName("www.runoob.com");
        }
catch (UnknownHostException e) {
System.exit(2);
        }
System.out.println(address.getHostName() + "=" + address.getHostAddress());
        System.exit(0);
    }
}

```

[原文链接](https://www.runoob.com/java/net-address.html)
