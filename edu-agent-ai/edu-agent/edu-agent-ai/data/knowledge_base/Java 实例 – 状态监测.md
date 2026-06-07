# Java 实例 – 状态监测

**题目描述**: 以下实例演示了如何通过继承 Thread 类并使用 currentThread.getName() 方法来监测线程的状态：

```java

class MyThread extends Thread{
boolean waiting= true;
   boolean ready= false;
   MyThread() {
}
public void run() {
String thrdName = Thread.currentThread().getName();
      System.out.println(thrdName + " starting.");
      while(waiting)
System.out.println("waiting:"+waiting); 
      System.out.println("waiting...");
      startWait(); 
      try {
Thread.sleep(1000);
      }
catch(Exception exc) {
System.out.println(thrdName + " interrupted.");
      }
System.out.println(thrdName + " terminating.");
   }
synchronized void startWait() {
try {
while(!ready) wait();
      }
catch(InterruptedException exc) {
System.out.println("wait() interrupted");
      }
}
synchronized void notice() {
ready = true;
      notify();
   }
}
public class Main {
public static void main(String args[])
throws Exception{
MyThread thrd = new MyThread();
      thrd.setName("MyThread #1");
      showThreadStatus(thrd);
      thrd.start();
      Thread.sleep(50);
      showThreadStatus(thrd);
      thrd.waiting = false;
      Thread.sleep(50); 
      showThreadStatus(thrd);
      thrd.notice();
      Thread.sleep(50);
      showThreadStatus(thrd);
      while(thrd.isAlive())
System.out.println("alive");
      showThreadStatus(thrd);
   }
static void showThreadStatus(Thread thrd) {
System.out.println(thrd.getName() + "Alive:=" + thrd.isAlive() + " State:=" + thrd.getState());
   }
}

```

[原文链接](https://www.runoob.com/java/thread-monitor.html)
