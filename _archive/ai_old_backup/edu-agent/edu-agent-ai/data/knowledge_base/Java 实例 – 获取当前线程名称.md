# Java 实例 – 获取当前线程名称

**题目描述**: 以下实例演示了如何通过继承 Thread 类并使用 getName() 方法来获取当前线程名称：

```java

public class TwoThreadGetName extends Thread {
public void run() {
for (int i = 0; i < 10; i++) {
printMsg();
      }
}
public void printMsg() {
Thread t = Thread.currentThread();
      String name = t.getName();
      System.out.println("name=" + name);
   }
public static void main(String[] args) {
TwoThreadGetName tt = new TwoThreadGetName();
      tt.start();
      for (int i = 0; i < 10; i++) {
tt.printMsg();
      }
}
}

```

[原文链接](https://www.runoob.com/java/thread-name.html)
