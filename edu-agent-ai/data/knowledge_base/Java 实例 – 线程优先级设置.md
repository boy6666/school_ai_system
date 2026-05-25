# Java 实例 – 线程优先级设置

**题目描述**: 以下实例演示了如何通过setPriority() 方法来设置线程的优先级：

```java

public class SimplePriorities extends Thread {
private int countDown = 5;
   private volatile double d = 0; 
   public SimplePriorities(int priority) {
setPriority(priority);
      start();
   }
public String toString() {
return super.toString() + ": " + countDown;
   }
public void run() {
while(true) {
for(int i = 1; i < 100000; i++)
d = d + (Math.PI + Math.E) / (double)i;
         System.out.println(this);
         if(--countDown == 0) return;
      }
}
public static void main(String[] args) {
new SimplePriorities(Thread.MAX_PRIORITY);
      for(int i = 0; i < 5; i++)
new SimplePriorities(Thread.MIN_PRIORITY);
   }
}

```

[原文链接](https://www.runoob.com/java/thread-getpri.html)
