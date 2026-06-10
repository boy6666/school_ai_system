```markdown
# Java 基础：从 Socket 通信看扎实的语法功底

很多同学一上来就直奔 TCP 三次握手、Socket 超时和多线程服务器，却常常因为 **Java 基础** 不牢而卡在莫名其妙的编译错误、资源泄漏或线程死锁上。本讲以经典的“客户端-服务器”通信为例，带你夯实四个最重要的 Java 基础支柱：**类组织与导入、异常处理、IO 流资源管理以及多线程入门**。学完后你会发现，所谓网络编程的棘手问题，八成源于基础细节没处理对。

---

## 1. 核心概念通俗讲解

**（1）类、包与导入——代码的“收纳箱”**  
Java 用 `package` 声明类所属的文件夹，用 `import` 引入其他类。写 Socket 程序时，必须导入 `java.net.*` 和 `java.io.*`，否则编译器会告诉你“找不到符号”。就像工具分门别类放在不同抽屉，不打开抽屉就拿不到扳手。

**（2）异常处理——给程序装上“安全气囊”**  
网络通信随时可能断连、超时，如果不用 `try-catch` 捕获 `IOException`，程序会直接崩溃。Java 强制检查异常（Checked Exception），提醒你必须处理潜在风险。比如 `ServerSocket` 的构造方法抛出 `IOException`，你不能假装它不存在。

**（3）IO 流与资源管理——数据的水管系统**  
客户端和服务器之间通过“流”传送数据：`InputStream`/`OutputStream` 处理字节，`Reader`/`Writer` 处理字符。关键原则是 **用完即关**，否则端口会被占满、内存会泄漏。Java 7 以后的 `try-with-resources` 可以自动关门，省心又安全。

**（4）多线程入门——让服务器同时招待多位客人**  
单线程服务器一次只能服务一个客户端：`accept()` 之后必须等当前客户端断开，才能接下一个。引入 `Thread` 或 `Runnable` 后，每个连接分配一条独立“工作线”，大家互不阻塞，这就是多线程服务器的雏形。

---

## 2. 可运行代码示例

### 示例 1：单线程回显服务器（演示 try-with-resources 与基础 IO）

```java
import java.io.*;
import java.net.*;

public class EchoServer {
    public static void main(String[] args) {
        // try-with-resources 自动关闭 ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            System.out.println("服务器启动，等待连接...");
            Socket client = serverSocket.accept();
            System.out.println("客户端已连接：" + client.getInetAddress());

            // 从客户端读，并向客户端回写
            try (
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(
                    client.getOutputStream(), true)
            ) {
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("收到：" + line);
                    out.println("Echo: " + line);
                }
            } // in, out 自动关闭
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### 示例 2：多线程服务器（演示 Runnable 接口与 Thread）

```java
import java.io.*;
import java.net.*;

public class MultiThreadServer {
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(8888);
        System.out.println("多线程服务器已启动...");
        while (true) {
            Socket client = server.accept();
            // 为每个客户端启动一个新线程
            new Thread(new ClientHandler(client)).start();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true)
        ) {
            String msg;
            while ((msg = in.readLine()) != null) {
                System.out.println(Thread.currentThread().getName()
                                   + " 收到：" + msg);
                out.println("Echo: " + msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

> **运行提示**：先用 `javac` 编译，启动服务器后，用 `telnet localhost 8888` 或浏览器测试，或配合简单的 `Socket` 客户端运行。

---

## 3. 易错点清单（3～5 个）

1. **流没有成对关闭**  
   `Socket` 的输入输出流若只关其中之一，或忘记关闭 `Socket`，会导致连接泄漏。**解药**：始终使用 `try-with-resources`，或 `finally` 块按顺序关闭。

2. **`readLine()` 阻塞至海枯石烂**  
   `BufferedReader.readLine()` 遇到对方未发送换行符时会永久阻塞，客户端以为自己崩溃了。解决办法：改用 `read()` 自定义协议，或使用 `setSoTimeout()` 设置超时。

3. **字节流与字符流混用导致乱码**  
   网络传输的是字节，若想读中文必须用 `InputStreamReader` 指定字符集（如 `UTF-8`），两端统一。不要在字节流上直接读写 `String`。

4. **单线程服务器阻塞所有后来者**  
   `accept()` 后的 `while` 循环处理一个客户端时，后续客户端无法连接。这是典型的“基础不牢，架构遭殃”。解决方案：学好多线程或 NIO，让服务器并发。

5. **异常被生吞活剥**  
   很多初学者写 `catch (IOException e) { }`，导致程序静静崩溃，找不到原因。最少应打印 `e.printStackTrace()`，或记录日志，不要留空。

---

## 4. 学习建议

- **画类图与流图**：拿起纸笔画出 `ServerSocket` → `Socket` → `InputStream`/`OutputStream` → `Reader`/`Writer` 的关系。理清“谁创建谁”后，代码自然成型。
- **强制使用 `try-with-resources`**：从今天起，不再手写 `close()`。这个习惯能避免 90% 的资源泄漏。
- **单线程→多线程→线程池逐步演进**：先把单线程回显跑通，再封装 `Runnable` 用 `new Thread`，最后引入 `Executors.newFixedThreadPool`。每次只增加一个环节，出错也不慌。
- **搭配网络抓包工具**：Wireshark 或 tcpdump 能让你直观看到 TCP 三次握手、数据传输和四次挥手，把 Java 代码与协议行为一一对应。
- **写一百行不如调试十行**：在 `readLine()` 前后打日志、用 `jstack` 查看线程状态，这些基础调试本领会让你的网络编程之路一马平川。

打好 Java 基础，TCP 握手、Socket 超时、多线程并发这些难题都将变成有章可循的填空题。现在就去动手运行上面的例子，改一改端口号、加一句超时设置，感受基础的力量吧。
```