# Java 实例 – 查看线程优先级

**题目描述**: 以下实例演示了如何使用 getThreadId() 方法获取线程id：

```java

public class Main extends Object {
private static Runnable makeRunnable() {
Runnable r = new Runnable() {
public void run() {
for (int i = 0; i < 5; i++) {
Thread t = Thread.currentThread();
               System.out.println("in run() - priority="
               + t.getPriority()+ ", name=" + t.getName());
               try {
Thread.sleep(2000);
               }
catch (InterruptedException x) {
}
}
}
};
      return r;
   }
public static void main(String[] args) {
System.out.println("in main() - Thread.currentThread().getPriority()=" + Thread.currentThread().getPriority());
      System.out.println("in main() - Thread.currentThread().getName()="+ Thread.currentThread().getName());
      Thread threadA = new Thread(makeRunnable(), "threadA");
      threadA.start();
      try {
Thread.sleep(3000);
      }
catch (InterruptedException x) {
}
System.out.println("in main() - threadA.getPriority()="+ threadA.getPriority());
   }
}

```

[原文链接](https://www.runoob.com/java/thread-priorityinfo.html)
