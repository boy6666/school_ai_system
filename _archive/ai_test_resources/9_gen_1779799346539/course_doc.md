# Java 基础核心概念与实战

## 一、通俗解释核心概念

Java 基础就像建造高楼的地基，所有复杂应用都构建于这些基本元素之上。可以把 Java 程序想象成一个用乐高积木搭建的世界——每一块积木就是一个“对象”，而把积木拼起来的规则就是“语法”。

**1. 类与对象**  
类是你画的设计图纸，对象是根据图纸造出来的实体。比如“手机”是一个类，你手中的 iPhone 和华为 Mate 就是两个不同的对象。每个对象有自己的属性（屏幕尺寸、颜色）和行为（打电话、拍照）。

**2. 数据类型**  
Java 把数据分成两类：基本类型（`int`, `double`, `boolean` 等）和引用类型（`String`, 数组, 自定义类）。基本类型像一个个简单的工具盒，直接装数值；引用类型则像一把钥匙，指向内存中实际存放数据的位置。

**3. 控制流程**  
顺序、选择（`if-else`, `switch`）、循环（`for`, `while`）这三种结构就像道路上的交通指挥，决定了代码执行的路线。没有它们，程序只能一条道走到黑。

**4. 异常处理**  
程序运行时的意外就是“异常”，比如文件找不到、网络中断。`try-catch-finally` 就像一个安全气囊，确保出问题时程序能够优雅处理，而不是直接崩溃。

**5. 输入输出（IO）**  
Java 通过“流”来读写数据，就像水管输送水一样。`InputStream`/`OutputStream` 处理字节，`Reader`/`Writer` 处理字符。后面要学的 Socket 通信就是网络 IO 的典型应用，本质上也是在这些流上操作。

掌握了这些基础，再去看 TCP 三次握手、多线程服务器就不会觉得飘在空中了——它们不过是把 Java 基本的类、异常、IO 组合起来，再加上了网络协议和多线程的知识而已。

---

## 二、两个可运行的 Java 代码示例

### 示例1：面向对象基础——学生成绩管理

```java
// 定义一个学生类
class Student {
    // 私有属性（封装）
    private String name;
    private double score;

    // 构造方法
    public Student(String name, double score) {
        this.name = name;
        this.score = score;
    }

    // 获取等级的方法
    public String getGrade() {
        if (score >= 90) return "优秀";
        else if (score >= 70) return "良好";
        else if (score >= 60) return "及格";
        else return "不及格";
    }

    // 展示信息
    public void display() {
        System.out.println(name + " 的成绩：" + score + " 等级：" + getGrade());
    }
}

public class BasicDemo {
    public static void main(String[] args) {
        // 创建对象并调用方法
        Student stu1 = new Student("小明", 85);
        Student stu2 = new Student("小红", 58);
        stu1.display();
        stu2.display();
    }
}
```

**运行结果：**
```
小明 的成绩：85.0 等级：良好
小红 的成绩：58.0 等级：不及格
```

**知识点覆盖：** 类与对象、封装、构造方法、`this` 关键字、条件判断、字符串输出。这些正是 Java 基础的核心。

### 示例2：基础 I/O 与异常处理——模拟简单聊天记录保存

```java
import java.io.*;
import java.util.Scanner;

public class ChatLogger {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("请输入本次聊天记录文件名：");
        String filename = scanner.nextLine();
        // 使用 try-with-resources 自动关闭流
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            System.out.println("开始记录聊天（输入 exit 结束）：");
            while (true) {
                System.out.print(">> ");
                String line = scanner.nextLine();
                if ("exit".equals(line)) {
                    break;
                }
                writer.write(line);
                writer.newLine();   // 写入换行符
                writer.flush();     // 立即刷盘
                System.out.println("已记录");
            }
            System.out.println("聊天记录已保存到：" + filename);
        } catch (IOException e) {
            // 异常处理
            System.err.println("文件操作出错：" + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}
```

**运行示例：**
```
请输入本次聊天记录文件名：chat.txt
开始记录聊天（输入 exit 结束）：
>> 你好
已记录
>> 今天天气不错
已记录
>> exit
聊天记录已保存到：chat.txt
```

**知识点覆盖：** 输入输出流（`FileWriter`, `BufferedWriter`）、异常捕获（`IOException`）、`finally` 块、字符串比较 `equals`、`Scanner` 使用。这些是后续学习 Socket 通信时直接复用的基础——服务器端读写客户端消息用的正是同一套 I/O 操作。

---

## 三、3～5个易错点

**1. 字符串比较用了 `==` 而不是 `equals()`**  
`==` 比较的是引用地址，`equals()` 比较内容。新手常写出 `if (str1 == str2)` 导致逻辑错误。建议统一使用 `"xxx".equals(str)` 可避免空指针。

**2. 基本类型包装类的“自动拆箱”空指针**  
`Integer num = null;` 后直接 `int n = num;` 会在运行时抛出 `NullPointerException`。拆箱前务必判空。

**3. 异常捕获范围过大或过小**  
`catch (Exception e)` 一把兜底会吞掉关键异常，难以定位问题；而只捕获极细的子类可能漏掉其他异常。应遵循“先具体，后一般”的顺序，并在 `catch` 块中记录日志而非空处理。

**4. 重载与重写混淆**  
重载是同一个类中方法名相同、参数列表不同；重写是子类对父类方法签名完全一致地重新实现。用 `@Override` 注解可以提前发现拼写错误。

**5. 静态方法“假重写”**  
父类静态方法无法被子类真正覆盖，调用哪个版本由引用类型决定。一律用类名调用静态方法可以避免歧义。

---

## 四、学习建议

**1. 从“画图纸”做起**  
每学一个新概念，立即写一个最短的小程序验证它。比如学了数组，就写个冒泡排序；学了继承，就造一个动物→猫狗的层次。代码量是内化基础的不二法门。

**2. 把异常当作朋友**  
不要害怕红色的错误信息，每一条异常都是 Java 在告诉你哪里出问题。养成阅读异常堆栈的习惯，从最上面的 `Caused by` 一层层向下追溯。

**3. 边学基础边瞄后续目标**  
你现在可能觉得线程、Socket 很难，但其实它们就是把基础的 I/O、类、异常处理组合起来。试着提前看一眼 TCP 三次握手的流程，再用今天学的 `FileWriter` 去理解 `Socket.getOutputStream()`，你会发现两者惊人地相似——都是流操作。这样学习既巩固基础，又为难点提前铺路。

**4. 重视“命名”和“缩进”**  
命名靠拼音、代码毫无缩进是后续痛苦的根源。从第一个 `HelloWorld` 就坚持驼峰命名、合理空行，三个月后你会感谢现在的自己。

基础打得越厚，后面的多线程服务器、超时处理这些高楼才能盖得越快。现在多写一行 `Student` 类，将来调试一次死锁就能省下半天。加油！