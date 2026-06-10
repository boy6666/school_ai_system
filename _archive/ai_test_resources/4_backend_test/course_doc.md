# Java 基础核心讲解

## 一、什么是 Java 基础

“Java 基础”并不只是记住 `int`、`for`、`class` 这些零散的关键词，而是理解 Java 语言**面向对象编程的本质**、**平台无关性的实现方式**以及**常用类库的正确用法**。它可以归纳为三大支柱：

1. **语法与结构**：变量、运算符、控制流、数组等基本元素。  
2. **面向对象**：封装、继承、多态、抽象类与接口。  
3. **常用 API**：字符串、集合框架、异常处理、I/O 流、多线程与网络编程。

Java 之所以强大，在于它把“万物皆对象”的思想贯彻到底，让代码更易维护和扩展。同时，Java 虚拟机（JVM）将源码编译成字节码，实现了“一次编写，到处运行”的跨平台能力。

## 二、可运行示例

### 示例 1：面向对象之多态

```java
// 父类
class Animal {
    public void sound() {
        System.out.println("动物发出声音");
    }
}

// 子类
class Dog extends Animal {
    @Override
    public void sound() {
        System.out.println("汪汪汪");
    }
}

class Cat extends Animal {
    @Override
    public void sound() {
        System.out.println("喵喵喵");
    }
}

public class PolymorphismDemo {
    public static void main(String[] args) {
        Animal myPet;          // 声明父类引用
        myPet = new Dog();     // 指向子类对象
        myPet.sound();         // 输出：汪汪汪
        myPet = new Cat();
        myPet.sound();         // 输出：喵喵喵
    }
}
```

**要点**：父类引用指向子类对象，调用方法时执行的是子类重写后的逻辑，这就是多态的核心。它让程序更灵活，符合“对扩展开放，对修改关闭”的设计原则。

### 示例 2：Socket 网络通信

参照经典的 Client-Server 模型，实现了一个简单的回声服务。

```java
// 服务器端
import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(8888);
        System.out.println("服务器已启动...");
        Socket client = ss.accept();
        System.out.println("客户端已连接：" + client.getInetAddress());

        BufferedReader in = new BufferedReader(
            new InputStreamReader(client.getInputStream()));
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);

        String msg = in.readLine();
        System.out.println("客户端说：" + msg);
        out.println("服务器收到：" + msg);

        client.close();
        ss.close();
    }
}

// 客户端
import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 8888);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));

        out.println("你好，服务器！");
        System.out.println("服务器回复：" + in.readLine());

        socket.close();
    }
}
```

运行：先启动 `Server`，再运行 `Client`。客户端发送消息，服务器接收后回复，展示了 `ServerSocket` 与 `Socket` 配合完成 TCP 通信的基本流程。

## 三、易错点

1. **字符串比较用 `==` 而不是 `equals()`**  
   `==` 比较的是引用地址，`equals()` 比较的是内容。除非是字符串常量池的巧合，否则 `new String("abc") == "abc"` 返回 `false`。

2. **在增强 for 循环中修改集合**  
   `for (String s : list) { list.remove(s); }` 会抛出 `ConcurrentModificationException`。应使用迭代器的 `remove()` 或普通 for 循环下标操作。

3. **基本类型与包装类的自动装箱拆箱陷阱**  
   两个 `Integer` 对象用 `==` 比较时，-128 到 127 之间因缓存为 true，范围外则为 false。强烈建议比较数值时使用 `equals()`。

4. **I/O 流未关闭导致资源泄露**  
   忘记关闭 `Stream`、`Reader`、`Socket` 等资源，会使文件句柄或端口无法释放。应使用 try-with-resources（Java 7+）自动关闭。

5. **异常捕获过于宽泛或直接忽略**  
   `catch (Exception e) {}` 会掩盖所有错误，让排错极难。应具体捕获，并至少记录日志或进行有意义的处理。

## 四、学习建议

- **夯实面向对象思想**：多画类图、多用继承和接口重构小项目，理解“高内聚、低耦合”的设计原则。
- **掌握集合框架的线程安全差异**：对比 `ArrayList` 和 `Vector`，`HashMap` 和 `ConcurrentHashMap`，动手写多线程测试，为后续线程安全学习打基础。
- **深入 JVM 内存模型**：了解栈、堆、方法区的关系，明白 `synchronized` 加锁的是什么对象，才能从根本上排查死锁。
- **多写可运行示例**：从 I/O、Socket 再到线程，每学一个知识点都亲手编译运行一次，错误是最好的老师。
- **养成良好编码习惯**：遵循 Java 命名规范，使用 try-with-resources，尽早使用 `equals()`、避免嵌套过深，代码不仅是给机器看的，更是给人看的。

把基础打得越扎实，后续理解同步、锁、JVM 调优等高阶主题就越轻松。