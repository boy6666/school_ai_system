# Java 实例 – 死锁及解决方法

**题目描述**: 死锁是这样一种情形：多个线程同时被阻塞，它们中的一个或者全部都在等待某个资源被释放。由于线程被无限期地阻塞，因此程序不可能正常终止。 java 死锁产生的四个必要条件： 当上述四个条件都成立的时候，便形成死锁。当然，死锁的情况下如果打破上述任何一个条件，便可让死锁消失。下面用java代码来模拟一下死锁的产生。 解决死锁问题的方法是：一种是用synchronized，一种是用Lock显式锁实现。 而如果不恰当的使用了锁，且出现同时要锁多个对象时，会出现死锁情况，如下： 为了解决这个问题，我们不使用显示的去锁，我们用信号量去控制。 信号量可以控制资源能被多少线程访问，这里我们指定只能被一个线程访问，就做到了类似锁住。而信号量可以指定去获取的超时时间，我们可以根据这个超时时间，去做一个额外处理。 对于无法成功获取的情况，一般就是重复尝试，或指定尝试的次数，也可以马上退出。 以上实例代码输出结构为：

```java

import java.util.Date;
 
public class LockTest {
public static String obj1 = "obj1";
   public static String obj2 = "obj2";
   public static void main(String[] args) {
LockA la = new LockA();
      new Thread(la).start();
      LockB lb = new LockB();
      new Thread(lb).start();
   }
}
class LockA implements Runnable{
public void run() {
try {
System.out.println(new Date().toString() + " LockA 开始执行");
         while(true){
synchronized (LockTest.obj1) {
System.out.println(new Date().toString() + " LockA 锁住 obj1");
               Thread.sleep(3000); // 此处等待是给B能锁住机会
synchronized (LockTest.obj2) {
System.out.println(new Date().toString() + " LockA 锁住 obj2");
                  Thread.sleep(60 * 1000); // 为测试，占用了就不放
}
}
}
} catch (Exception e) {
e.printStackTrace();
      }
}
}
class LockB implements Runnable{
public void run() {
try {
System.out.println(new Date().toString() + " LockB 开始执行");
         while(true){
synchronized (LockTest.obj2) {
System.out.println(new Date().toString() + " LockB 锁住 obj2");
               Thread.sleep(3000); // 此处等待是给A能锁住机会
synchronized (LockTest.obj1) {
System.out.println(new Date().toString() + " LockB 锁住 obj1");
                  Thread.sleep(60 * 1000); // 为测试，占用了就不放
}
}
}
} catch (Exception e) {
e.printStackTrace();
      }
}
}

```

[原文链接](https://www.runoob.com/java/thread-deadlock.html)
