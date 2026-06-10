# Java 基础巩固：从面向对象到网络编程实践

## 一、核心概念通俗解读

**Java 基础** 不仅是语法规则的罗列，更是构建一切 Java 程序的骨架。这里我们聚焦几个最核心的模块：

- **类与对象**：Java 用类（`class`）定义事物的模板，对象（`new` 出来的实例）是真正干活的主体。所有代码都必须放在类内部。
- **异常处理**：程序运行时可能出现的“意外情况”，用 `try-catch` 捕获并处理，避免程序崩溃。`IOException` 是输入输出异常家族的总称。
- **I/O 流**：数据像水流一样在程序与外部（文件、网络）之间传输。`InputStream`/`OutputStream` 处理字节，`Reader`/`Writer` 处理字符，`BufferedReader` 可以一次读一行，方便高效。
- **多线程**：让程序“同时”做多件事。通过继承 `Thread` 或实现 `Runnable` 接口实现，网络服务器常用多线程，为每个客户端分配一个独立线程提供服务。
- **网络编程基础**：`Socket` 代表两台机器之间的“电话线”，`ServerSocket` 在服务器端“接听”并创建 `Socket`。数据通过流在这条线上双向传递。

下面通过两个可运行的网络通信示例，把上述概念串联起来，真正理解 Java 基础在实际项目中的运用。

## 二、可运行代码示例

### 示例1：单线程回显服务器与客户端（演示异常、IO流、Socket基础）

**服务器端** 创建 `ServerSocket` 监听端口，接收一个客户端连接，读取一行消息并回送响应。

```java
import java.io.*;
import java.net.*;

public class EchoServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            System.out.println("服务器启动，等待连接...");
            Socket clientSocket = serverSocket.accept();           // 阻塞，直到有客户端连接
            System.out.println("客户端已连接：" + clientSocket.getInetAddress());

            // 字符流包装字节流，方便读写字符串
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(clientSocket.getOutputStream()));

            String message = in.readLine();                       // 读取一行
            System.out.println("客户端说：" + message);
            out.write("服务器回复: " + message + "\n");
            out.flush();                                          // 强制写出

            // 关闭资源
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

**客户端** 连接到服务器，发送一句话，并设置读取超时，防止无限等待。

```java
import java.io.*;
import java.net.*;

public class EchoClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8888)) {
            socket.setSoTimeout(3000);                // 读取超时 3 秒（重要！）
            BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            out.write("你好，服务器！\n");
            out.flush();
            String response = in.readLine();          // 可能超时
            System.out.println("服务器回应：" + response);

            out.close();
            in.close();
        } catch (SocketTimeoutException e) {
            System.out.println("读取超时，服务器无响应");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### 示例2：多线程服务器（巩固多线程与Socket超时）

为每个客户端启动一个新线程，使服务器可以同时处理多个连接，并在线程内部设置读取超时。

```java
import java.io.*;
import java.net.*;

public class MultiThreadServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("多线程服务器已启动...");
        while (true) {
            Socket clientSocket = serverSocket.accept();
            new ClientHandler(clientSocket).start();     // 启动线程处理连接
        }
    }
}

class ClientHandler extends Thread {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream()))
        ) {
            socket.setSoTimeout(5000);               // 线程内设置读超时 5 秒
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(Thread.currentThread().getName() + " 收到: " + message);
                out.write("回显: " + message + "\n");
                out.flush();
            }
        } catch (SocketTimeoutException e) {
            System.out.println("客户端超时，线程结束");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

客户端可直接复用示例1的客户端，打开多个终端运行，即可测试多线程服务器的并发能力。

## 三、易错点警示（3～5个）

1. **资源泄漏**：`Socket`、各类流对象使用后必须关闭，否则会导致端口占用和内存泄漏。推荐使用 `try-with-resources`（示例2）或在 `finally` 块中逐一关闭。
2. **死等阻塞**：`readLine()` 在没有数据时会一直阻塞。如果网络异常，客户端可能永远卡死。务必调用 `setSoTimeout()` 设置超时，并捕获 `SocketTimeoutException`。
3. **多线程同步缺失**：如果多个线程修改同一共享资源（如统计在线人数），必须使用 `synchronized` 或 `Lock`，否则会造成数据错乱。
4. **流乱码问题**：网络传输默认使用字节流，包装为字符流时必须指定一致编码（如 `UTF-8`）。示例中未指定，会使用平台默认编码，不同平台可能导致乱码，建议显式使用 `InputStreamReader(stream, StandardCharsets.UTF_8)`。
5. **异常吞噬**：`catch (Exception e)` 过于宽泛，可能掩盖具体错误，不利于调试。应精确捕获 `IOException`、`SocketTimeoutException` 等。

## 四、学习建议

1. **动手编码**：将上述两个示例完整敲入IDE，修改端口、消息、超时时间，观察现象，在出错中加深理解。
2. **画图梳理流程**：画出客户端–服务器的交互时序图，标出 `accept()`、`readLine()`、`write()` 的调用顺序和阻塞点。
3. **阅读JDK源码**：查看 `Socket`、`ServerSocket` 的文档，理解每个方法的含义和超时设置。
4. **系统学习基础**：先扎实掌握异常体系、IO流类层次、多线程的两种实现方式，网络编程是这些基础的“联合作战”。
5. **写日志代替 `printStackTrace`**：养成用日志框架记录异常的习惯，为日后项目铺路。

Java 基础是内功，网络编程是极佳的试炼场。把基础打牢，后续学习 TCP 握手细节、高并发框架才能游刃有余。