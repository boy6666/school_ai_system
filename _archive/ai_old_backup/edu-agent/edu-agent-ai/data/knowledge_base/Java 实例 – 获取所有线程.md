# Java 实例 – 获取所有线程

**题目描述**: 以下实例演示了如何使用 getName() 方法获取所有正在运行的线程：

```java

public class Main extends Thread {
public static void main(String[] args) {
Main t1 = new Main();
      t1.setName("thread1");
      t1.start();
      ThreadGroup currentGroup = 
      Thread.currentThread().getThreadGroup();
      int noThreads = currentGroup.activeCount();
      Thread[] lstThreads = new Thread[noThreads];
      currentGroup.enumerate(lstThreads);
      for (int i = 0; i < noThreads; i++)
System.out.println("线程号：" + i + " = " + lstThreads[i].getName());
   }
}

```

[原文链接](https://www.runoob.com/java/thread-showall.html)
