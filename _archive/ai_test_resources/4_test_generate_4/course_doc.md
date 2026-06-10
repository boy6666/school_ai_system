# Java 基础：从 Socket 通信理解 I/O、多线程与同步

Java 基础中的网络编程、I/O 流和线程安全是构建实际应用的基石。本文通过经典的 `ServerSocket` 与 `Socket` 通信实例，通俗解释这些核心概念，并逐步深入到多线程场景下的同步问题，帮助你扫清薄弱点。

## 核心概念通俗解释

### 1. Socket 通信模型
可以把 **Socket（套接字）** 想象成两台计算机之间的“电话”。一台作为服务器，使用 `ServerSocket` 监听指定的端口号（比如 8888），等待“电话”接入；另一台作为客户端，使用 `Socket` 主动拨打服务器的 IP 和端口。一旦连接建立，双方就能通过输入输出流（`getInputStream()` / `getOutputStream()`）进行对话，就像通过话筒和听筒交流一样。

### 2. I/O 流的封装
原始的字节流 `InputStream` 和 `OutputStream` 读写效率低且不方便。Java 基础中常用 **BufferedReader** 和 **BufferedWriter** 进行包装：
- `BufferedReader` 提供 `readLine()` 方法，能一次读取一行文本，内部自带缓冲区，减少磁盘或网络访问次数。
- `BufferedWriter` 同理，通过 `write()` 和 `newLine()` 写入数据，最后调用 `flush()` 强制刷出缓冲区。
- 为了处理字符编码，常配合 `InputStreamReader` 和 `OutputStreamWriter`，将字节流转换为字符流。

### 3. 多线程与线程安全
单线程服务器一次只能处理一个客户端，其他客户端必须排队。这就像餐厅只有一个服务员。引入 **多线程** 后，主线程负责监听，每来一个客户端就分配一个新的工作线程来服务，多个客户端可以同时被响应。  
但多线程共享数据时会产生 **线程安全** 问题，比如多个线程同时修改一个共享计数器，结果可能出错。Java 使用 **synchronized** 关键字来保证同一时刻只有一个线程执行某段代码，其原理是在对象上加锁（监视器锁），从而避免数据竞争。

## 可运行代码示例

### 示例 1：单线程 Echo 服务器与客户端
**服务器端**，接受客户端消息并原样返回（补全参考知识库的示例）：
```java
import java.io.*;
import java.net.*;

public class SingleThreadServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("服务器启动，等待连接...");
        Socket socket = serverSocket.accept();
        System.out.println("客户端已连接：" + socket.getInetAddress());
        
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream()));
        
        String message = reader.readLine();
        System.out.println("收到客户端消息：" + message);
        
        writer.write("服务器回声：" + message);
        writer.newLine();
        writer.flush();
        
        reader.close();
        writer.close();
        socket.close();
        serverSocket.close();
    }
}
```
**客户端**：
```java
import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 8888);
        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream()));
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        
        writer.write("你好，服务器！");
        writer.newLine();
        writer.flush();
        
        String response = reader.readLine();
        System.out.println("服务器响应：" + response);
        
        writer.close();
        reader.close();
        socket.close();
    }
}
```

### 示例 2：多线程服务器与 synchronized 应用
为了让服务器并发处理多个客户端，我们为每个连接创建新线程，并用 `synchronized` 保护共享的在线人数计数器。
```java
import java.io.*;
import java.net.*;

public class MultiThreadServer {
    private static int onlineCount = 0;          // 共享资源

    public static synchronized void addCount() { // 同步方法
        onlineCount++;
        System.out.println("当前在线：" + onlineCount);
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("多线程服务器启动...");
        
        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket)).start();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        public ClientHandler(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream()))) {
                
                addCount();   // 线程安全地增加计数
                String msg = reader.readLine();
                writer.write("多线程回复：" + msg);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```
客户端代码与示例1相同，可同时启动多个客户端测试并发处理效果。

## 易错点

1. **资源未关闭与内存泄漏**  
   `Socket`、`BufferedReader`、`BufferedWriter` 等资源必须在使用后关闭，否则会占用系统资源。推荐使用 `try-with-resources`（示例2中已使用）来自动关闭。

2. **忘记调用 `flush()`**  
   `BufferedWriter` 写数据时，数据可能停留在缓冲区，若未调用 `flush()` 或 `close()`（它也会刷出），对方可能永远收不到消息。

3. **阻塞方法导致假死**  
   `accept()` 会阻塞直到有客户端连接；`readLine()` 会阻塞直到读到换行符。若不发送换行符或连接意外断开，程序可能卡住。务必约定通信协议，比如每条消息以换行符结束。

4. **多线程下共享变量不同步**  
   示例2中的 `onlineCount` 若不加 `synchronized`，多个线程同时执行 `onlineCount++` 会导致计数错误（这个自增操作并非原子操作）。理解 `synchronized` 的锁对象原则，避免死锁风险。

5. **字符编码不统一**  
   `InputStreamReader` 默认使用平台编码，跨平台可能乱码。显式指定编码（如 `StandardCharsets.UTF_8`）是最稳妥的做法。

## 学习建议

- **动手强于阅读**：将两个示例完整敲一遍，逐一修改代码测试上述易错点，比如去掉 `flush()` 观察现象，体会原理。
- **分步理解多线程**：先懂单线程，再引入多线程；画出线程间共享与独立的数据，用日志输出线程名辅助调试。
- **深入 synchronized**：学习对象锁、类锁、同步块和同步方法的区别，写一些小demo模拟并发计数器，并故意制造数据竞跑，用 `jstack` 观察锁状态。
- **拓展迁移**：把学到的 I/O 流和线程知识迁移到文件日志系统或简单的聊天室项目，巩固 `Java 基础` 的同时，建立完整项目观感。