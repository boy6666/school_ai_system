# Java 实例 – 终止线程

**题目描述**: Java 中原来在 Thread 中提供了 stop() 方法来终止线程，但这个方法是不安全的，所以一般不建议使用。 本文向大家介绍使用 interrupt 方法中断线程。 使用 interrupt 方法来中断线程可分为两种情况： 在第一种情况下使用interrupt方法，sleep方法将抛出一个InterruptedException例外，而在第二种情况下线程将直接退出。下面的代码演示了在第一种情况下使用interrupt方法。

```java

public class ThreadInterrupt extends Thread
{
public void run()
{
try
{
sleep(50000);  // 延迟50秒 
}
catch (InterruptedException e)
{
System.out.println(e.getMessage()); 
        }
}
public static void main(String[] args) throws Exception
{
Thread thread = new ThreadInterrupt(); 
        thread.start(); 
        System.out.println("在50秒之内按任意键中断线程!"); 
        System.in.read(); 
        thread.interrupt(); 
        thread.join(); 
        System.out.println("线程已经退出!"); 
    }
}

```

[原文链接](https://www.runoob.com/java/thread-stop.html)
