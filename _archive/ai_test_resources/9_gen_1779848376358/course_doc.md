# Java 基础核心要点讲解

Java 基础是构建任何 Java 程序的基石，无论你未来是编写简单的控制台工具，还是开发复杂的多线程服务器，都必须扎实掌握数据类型、控制流程、面向对象和字符串处理等基本概念。下面我们用通俗的语言梳理核心知识，并配合可运行的代码示例和常见错误提点，帮助你在网络编程（比如 Socket 通信）中少踩坑。

## 一、核心概念通俗解读

Java 是一种**强类型、面向对象**的语言。我们把现实世界的事物抽象成 **类（class）**，用 **对象（Object）** 来代表具体个体。例如，一个服务器程序可能有一个 `Server` 类，里面包含启动、监听等方法。

**基本数据类型**（int、double、boolean 等）直接存储数值，而**引用类型**（如 String、数组、自定义类）存储的是对象在内存中的地址。尤其要注意 `String` 是不可变的，每次修改都会产生新对象。

在服务器端编程中经常要读取客户端发来的消息，这些消息本质就是**字符串**。如果字符串处理不扎实，解析协议、判断命令就会错误百出。

## 二、代码示例

### 示例 1：字符串反转与判空（模拟服务器消息处理）

```java
public class StringUtil {
    /**
     * 安全反转字符串，处理 null 和空串
     */
    public static String reverseSafe(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input);
        return sb.reverse().toString();
    }

    public static void main(String[] args) {
        String clientMessage = "hello server";
        String response = reverseSafe(clientMessage);
        System.out.println("服务器返回: " + response);

        // 测试 null 情况
        String nullMsg = null;
        System.out.println("反转 null: " + reverseSafe(nullMsg));
    }
}
```

**说明**：这个示例展示了字符串空值安全处理及 `StringBuilder` 的用法。在服务器读取客户端数据时，消息可能为 `null`（例如连接意外断开），直接调用 `input.length()` 会抛出 `NullPointerException`。

### 示例 2：基本类型与引用类型对比

```java
public class TypeDemo {
    public static void main(String[] args) {
        int a = 100;
        int b = 100;
        System.out.println(a == b);  // true，基本类型比值

        String s1 = new String("hello");
        String s2 = new String("hello");
        System.out.println(s1 == s2);        // false，比较的是地址
        System.out.println(s1.equals(s2));   // true，比较的是内容

        // 自动装箱陷阱
        Integer i1 = 128;
        Integer i2 = 128;
        System.out.println(i1 == i2);        // false (超出缓存范围 -128~127)
    }
}
```

**说明**：该代码揭示了 `==` 与 `equals` 的区别，以及整型自动装箱时的缓存边界问题。在比较客户端发的端口号、状态码等数字时，若使用 `Integer` 对象用 `==` 比较，很可能在数值大于 127 时出现意外 `false`。

## 三、易错点整理

1. **字符串比较用 `==` 而不用 `equals`**  
   `==` 比较两个字符串对象的内存地址，`equals` 才比较字符序列。Socket 通信中收到的指令（如 `“LOGIN”`）必须用 `equals` 判断，否则命令永远不会匹配成功。

2. **空指针异常（NullPointerException）**  
   调用 `null` 对象的任何方法都会抛出该异常。从 `Socket.getInputStream()` 读取数据时，如果客户端意外关闭，`readLine()` 可能返回 `null`，直接调用它的方法必然崩溃。务必先判断是否为 `null`。

3. **基本类型与包装类的自动装箱边界**  
   `Integer`、`Byte`、`Short`、`Long` 在 -128~127 范围内会复用缓存对象，使用 `==` 可能得到 `true`；超出该范围则创建新对象，`==` 变 `false`。网络编程中传输状态码、长度值等，如果用包装类比较，建议统一使用 `equals` 或转化为基本类型。

4. **String 不可变性带来的性能问题**  
   在循环中反复用 `+` 拼接字符串会产生大量临时对象，拖慢服务器响应。应该使用 `StringBuilder` 或 `StringBuffer`（线程安全）进行拼接。

5. **方法重写（Override）时未加 `@Override` 注解**  
   比如想重写 `equals` 方法却写成了 `public boolean equals(MyClass obj)`，这实际上是重载而非重写，导致集合（如 `HashMap`）行为异常。必须参数类型为 `Object`，并添上 `@Override` 注解让编译器帮忙检查。

## 四、学习建议

- **动手编码验证**：每学一个基础概念，立刻写短小的 `main` 方法测试。比如学完字符串池，就画图并用 `==` 和 `equals` 检测各种场景。
- **结合网络编程练习**：用课堂上的 `ServerSocket` 示例，自己写一个简单的“回显服务器”，在读取客户端消息后尝试反转、校验格式，这样既能巩固字符串处理，又能理解 TCP 字节流与字符的转换边界。
- **培养防御性编程习惯**：对于任何外部输入（用户、网络、文件），坚持先做 `null` 和空值判断。写工具方法时也提供安全版本，如示例 1 所示。
- **使用现代 Java 特性辅助**：学习 `Optional`、`NIO` 的 `Charset` 显示指定编码等，但要先打牢基础，再逐步升级技能。

把最基础的字符串、数据类型和控制流弄通，后续的多线程服务器、三次握手理解才能站得住脚。现在就开始写代码实践吧！