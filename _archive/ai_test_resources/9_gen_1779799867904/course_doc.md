# Java 网络编程基础：ServerSocket 与 Socket 通信核心解析

Java 的网络编程能力封装在 `java.net` 包中，它把复杂的 TCP 协议细节抽象成两个核心类：`ServerSocket` 和 `Socket`。理解这两个类，是写出稳定、高效的客户端/服务器程序的第一步。

## 1. 核心概念通俗解释

- **IP 地址与端口**  
  IP 地址像一座大楼的门牌号，端口则是楼内某个房间的号码。一台机器上可以同时运行多个网络程序（如 Web 服务器、数据库），它们通过不同的端口号区分。

- **TCP 连接的建立（三次握手）**  
  三次握手是 TCP 协议在通信前建立可靠连接的过程，可以类比为打电话：  
  ① 客户端拨号：“你好，能听到吗？”(SYN)  
  ② 服务器接听：“听到了，你能听到我吗？”(SYN+ACK)  
  ③ 客户端回应：“能听到，我们开始通话吧。”(ACK)  
  在 Java 中，负责接听“电话”的是 `ServerSocket`，当客户端请求到达时，`ServerSocket.accept()` 方法返回一个 `Socket` 对象，代表一条已建立好的连接。这个 `accept()` 会阻塞，直到连接建立成功，背后的三次握手由底层 TCP 协议栈自动完成，无需我们手动实现。

- **Socket 与数据流**  
  `Socket` 相当于电话听筒，两端的 `getInputStream()` 和 `getOutputStream()` 分别是“听筒”和“话筒”。通过包装成字符流（如 `BufferedReader`/`BufferedWriter`），我们就可以用 `readLine()` 和 `write()` 进行文本对话；若传输二进制数据，则使用 `DataInputStream` 等字节流。

- **超时与多线程**  
  默认的 `accept()` 和 `read()` 会无限期等待。在实际项目中，必须设置超时时间，避免服务僵死。同时，单线程服务器同一时间只能服务一个客户端，其他客户端必须排队。为此需要引入多线程：每 accept 到一个连接，就交给一个工作线程处理，主线程继续监听新连接。

## 2. 可运行代码示例

### 示例 1：基础的单次一问一答（展示连接与流操作）
```java
// 服务器端（单次对话后退出）
import java.io.*;
import java.net.*;

public class BasicServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("服务器启动，等待连接...");
        Socket socket = serverSocket.accept();
        System.out.println("客户端连接成功：" + socket.getInetAddress());

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream()));

        String message = reader.readLine();
        System.out.println("收到客户端消息：" + message);
        writer.write("服务器已收到：" + message + "\n");
        writer.flush();

        reader.close();
        writer.close();
        socket.close();
        serverSocket.close();
    }
}
```
```java
// 客户端
import java.io.*;
import java.net.*;

public class BasicClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 8888);
        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream()));
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));

        writer.write("你好，服务器！\n");
        writer.flush();
        System.out.println("服务器回复：" + reader.readLine());

        writer.close();
        reader.close();
        socket.close();
    }
}
```

### 示例 2：支持超时与多线程的并发服务器
```java
// 服务器端（多线程，每次对话处理一个线程，并设置超时）
import java.io.*;
import java.net.*;

public class ConcurrentServer {
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
        ClientHandler(Socket socket) { this.socket = socket; }
        public void run() {
            try {
                socket.setSoTimeout(10000); // 读超时10秒，避免僵死
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(
                    socket.getOutputStream(), true);
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("收到：" + line);
                    writer.println("Echo: " + line);
                }
            } catch (SocketTimeoutException e) {
                System.err.println("连接超时，关闭线程");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}
```
客户端代码只需将 `BasicClient` 改为发送多行数据，或直接通过 `telnet localhost 8888` 交互测试。

## 3. 易错点总结

1. **忘记刷新缓冲区**  
   `BufferedWriter.write()` 后若不调用 `flush()`，数据可能滞留在内存中不会被真正发送，客户端将永远阻塞在 `readLine()` 上，造成 **死锁** 般的效果。推荐使用 `PrintWriter` 的自动刷新特性。

2. **资源未释放导致端口占用**  
   关闭的顺序应为：先关闭流，再关闭 `Socket`，最后关闭 `ServerSocket`。若未在 `finally` 中关闭，一旦发生异常就可能导致端口 `8888` 被占用，下次启动报 `BindException`。

3. **readLine() 阻塞与断开检测**  
   `readLine()` 读到 `null` 表示客户端已断开连接。如果代码没有判断 `null`，可能会无限循环输出 `null` 或抛出异常。同时，客户端断网而服务端未检测到时，`readLine()` 可能永久阻塞，需配合超时 (`setSoTimeout`) 来定期“唤醒”。

4. **单线程无法处理并发**  
   示例1中的服务器只能服务一个客户端，后续连接只能排队。学习阶段很容易忘记将 `accept()` 放入循环并启动多线程，导致测试时第二个客户端始终无响应。

5. **混用字节流与字符流**  
   Socket 底层是字节流，包装字符流时必须统一字符编码，否则中文会出现乱码。默认的 `InputStreamReader` 使用系统编码，最好显式指定 UTF-8：  
   `new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)`。

## 4. 学习建议

- **结合抓包理解三次握手**  
  下载 Wireshark，运行示例程序，观察 SYN → SYN+ACK → ACK 三个报文，以及 FIN 挥手过程。这能清晰地帮你建立“Java 阻塞 API 背后的 TCP 动作”心智模型。

- **从单线程→线程池→NIO 逐步演进**  
  先掌握阻塞式单线程通信，再引入 `new Thread()`；理解线程开销后，学习 `ExecutorService` 线程池；最后，如果对高并发感兴趣，可进一步接触 NIO 的 `Selector`。这是 Java 网络编程的标准化学习路径。

- **刻意练习异常处理和资源释放**  
  每个网络程序都应当用 `try-with-resources` 管理流，对 `Socket` 在 `finally` 中关闭。写完后故意拔掉网线或重启客户端，观察能否优雅退出，提高程序的健壮性。

- **用自建聊天室巩固多线程**  
  实现一个简单的群聊服务器：用一个集合保存所有客户端的输出流，把任一客户端发来的消息转发给所有其他人。这个实战项目将综合锻炼上述所有知识点，强烈推荐。