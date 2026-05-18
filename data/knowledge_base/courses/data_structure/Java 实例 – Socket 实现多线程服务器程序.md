# Java 实例 – Socket 实现多线程服务器程序

**题目描述**: 在 Java 中，实现一个多线程服务器程序可以通过使用ServerSocket来监听客户端连接，每当有新的客户端连接时，启动一个新的线程来处理该连接。下面是一个示例代码，展示了如何使用 Java Socket 实现一个多线程服务器程序。 首先，我们创建一个服务器端程序，它会监听指定的端口，并为每个客户端连接启动一个新的线程来处理通信。 客户端代码用于连接服务器并发送消息。可以创建多个客户端来测试服务器的多线程处理能力。 这种实现方式确保了服务器能够同时处理多个客户端连接，而不会因为一个客户端的长时间操作而阻塞其他客户端的请求。 打开终端或命令提示符，切换到存上述 Java 文件的目录，然后编译代码： 在编译完成后，首先运行服务器程序，服务器将启动并开始监听端口 12345。 打开另一个终端或命令提示符窗口，运行客户端程序。你可以运行多个客户端实例来测试多线程处理。 然后，你可以在客户端控制台中输入消息，例如： 服务器端的输出会显示：

```java

import java.io.*;
import java.net.*;
 
public class MultiThreadedServer {
public static void main(String[] args) {
int port = 12345; // 定义服务器端口
try (ServerSocket serverSocket = new ServerSocket(port)) {
System.out.println("服务器已启动，等待客户端连接...");
 
            while (true) {
Socket clientSocket = serverSocket.accept(); // 接受客户端连接
System.out.println("客户端已连接: " + clientSocket.getInetAddress().getHostAddress());
 
                // 为每个客户端连接启动一个新的线程
ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
} catch (IOException e) {
e.printStackTrace();
        }
}
}
class ClientHandler implements Runnable {
private Socket clientSocket;
 
    public ClientHandler(Socket socket) {
this.clientSocket = socket;
    }
 
    @Override
public void run() {
try (
InputStream input = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            PrintWriter writer = new PrintWriter(output, true)
) {
String clientMessage;
            while ((clientMessage = reader.readLine()) != null) {
System.out.println("收到客户端消息: " + clientMessage);
                writer.println("服务器回应: " + clientMessage); // 发送回应消息给客户端
}
} catch (IOException e) {
e.printStackTrace();
        } finally {
try {
clientSocket.close();
            } catch (IOException e) {
e.printStackTrace();
            }
}
}
}

```

[原文链接](https://www.runoob.com/java/net-multisoc.html)
