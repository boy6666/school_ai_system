# Java 实例 – 方法重载

**题目描述**: 先来看下方法重载（Overloading）的定义：如果有两个方法的方法名相同，但参数不一致，哪么可以说一个方法是另一个方法的重载。
具体说明如下： 以下实例演示了如何重载 MyClass 类的 info 方法：

```java

class MyClass {
int height;
    MyClass() {
System.out.println("无参数构造函数");
        height = 4;
    }
MyClass(int i) {
System.out.println("房子高度为 " + i + " 米");
        height = i;
    }
void info() {
System.out.println("房子高度为 " + height + " 米");
    }
void info(String s) {
System.out.println(s + ": 房子高度为 " + height + " 米");
    }
}
public class MainClass {
public static void main(String[] args) {
MyClass t = new MyClass(3);
        t.info();
        t.info("重载方法");
        //重载构造函数
new MyClass();
    }
}

```

[原文链接](https://www.runoob.com/java/method-overloading.html)
