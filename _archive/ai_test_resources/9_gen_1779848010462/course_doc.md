# Java 基础：从字符串与 I/O 看网络通信的基石

如果说网络编程是一栋高楼，那么 **Java 基础**就是最底层的钢筋骨架。你未来要面对的 Socket 通信、多线程服务器，本质上都是由最基础的字符串处理、I/O 流操作和异常控制组合而成。本讲不追求大而全，而是聚焦于**你即将反复使用的核心能力**，帮你扫清后续学习中的“地雷”。

## 一、核心概念：为通信而生

### 1. 一切都是“流”——I/O 流的思维方式
在 Java 中，数据像水流一样在不同节点间流动。读取数据用输入流（InputStream / Reader），写出数据用输出流（OutputStream / Writer）。网络编程的本质就是把 **Socket 的输出流**当成管道，把数据“灌”进去，再从另一端“接”出来。

### 2. 字符串——数据交换的唯一“语言”
网络上传输的绝大多数信息是文本，而 Java 的 `String` 对象就是文本载体。你需要熟悉它的三件套：**构造**（`new String(bytes)`）、**转换**（`getBytes()`）、**判等**（`equals()`），否则接收到的消息会变成乱码或被误判为“假相等”。

### 3. 异常处理——通信必须穿上的“防弹衣”
网络随时会断，文件可能不存在。`try-catch-finally`（或 `try-with-resources`）能保证无论发生什么意外，流都会被关闭，端口都会被释放，否则资源泄漏会让你的服务器“慢性死亡”。

## 二、可运行代码示例

### 示例 1：字符串“陷阱”与正确操作
```java
public class StringDemo {
    public static void main(String[] args) {
        // 1. 字符串比较：必须用 equals ！
        String cmd = new String("LOGIN");
        if ("LOGIN".equals(cmd)) {   // 推荐常量写前面，避免空指针
            System.out.println("收到登录指令");
        }
        
        // 2. StringBuilder 解决拼接低效
        StringBuilder sb = new StringBuilder();
        sb.append("客户端:").append("192.168.1.1").append(" 已上线");
        System.out.println(sb.toString());
    }
}
```
**预期输出**：  
```
收到登录指令  
客户端:192.168.1.1 已上线
```

### 示例 2：模拟“读取客户端消息”的控制台 I/O
```java
import java.io.*;

public class ConsoleIO {
    public static void main(String[] args) {
        // try-with-resources 自动关闭流
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));
             BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(System.out))) {
            
            System.out.print("请输入消息: ");
            String message = reader.readLine();
            
            // 回显，模拟服务器应答
            writer.write("服务端已收到: " + message);
            writer.newLine();
            writer.flush();   // 必须刷新，否则数据停留在缓冲区
        } catch (IOException e) {
            System.err.println("I/O 错误: " + e.getMessage());
        }
    }
}
```
**运行效果**：输入 `hello` → 服务端已收到: hello  

> 你会发现，这和你参考资料中 `ServerSocket` 示例里的 `BufferedReader`、`BufferedWriter` 完全一致，网络编程只是把 `System.in` 换成了 `socket.getInputStream()`。

## 三、易错点清单（3 大高频“炸点”）

1. **字符串对比用 `==`**  
   `==` 比较的是内存地址，不是内容。网络接收到的字符串永远是 `new` 出来的，必须用 `equals()`。凡是看到 `if (msg == "QUIT")` 都要警惕。

2. **写完流不刷新 (`flush`)**  
   `BufferedOutputStream`/`BufferedWriter` 都会先存满缓冲区再发送。不调用 `flush()` 或 `close()`，数据会一直卡在本地，对方永远等不到响应，最终触发**超时**。

3. **未处理 `NullPointerException`**  
   读取流时，`readLine()` 返回 `null` 表示对方已断开。若直接对 `null` 调用 `equals()` 或 `trim()`，服务器当场崩溃。正确做法永远是“先判空，后操作”。

4. **流关闭顺序错误**  
   应先关闭外层包装流（如 `BufferedReader`），再关闭底层 `Socket`。更安全的方式是始终用 `try-with-resources`，让编译期自动生成正确的关闭逻辑。

5. **字符编码假设为平台默认**  
   不显式指定 `UTF-8` 编码，在 Windows 和 Linux 之间传输中文会变成乱码。使用 `InputStreamReader(input, StandardCharsets.UTF_8)` 来杜绝此类问题。

## 四、学习建议

- **以“协议解析”为练手目标**：尝试用 `Scanner` 或 `split` 解析一条 `LOGIN:alice:123456` 这样的字符串，这其实就是最简单的自定义协议。
- **养成“防御式编程”习惯**：每写一行 I/O 代码，都问自己“如果这里读到的内容是 `null` 怎么办？”“如果网络断了会抛什么异常？”
- **先本地文件，再上网络**：用 `FileReader` 和 `FileWriter` 完全掌握流的开启、读取、关闭、异常处理，再过渡到 `Socket` 的流会顺畅很多。
- **结合调试工具**：使用 IDE 的断点，观察 `inputLine` 的实际值，你会对 `null` 和空字符串的区别有刻骨铭心的理解。

当这些基础成为肌肉记忆后，你在处理 TCP 三次握手、Socket 超时、多线程并发时，才能把全部精力放在网络逻辑本身上，而不是被一个字符串比较打得措手不及。现在，打开你的 IDE，把上面两个例子跑起来，亲手制造一次 `NullPointerException`，你才能真正记住它。