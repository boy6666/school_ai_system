# Java 实例 – 多线程异常处理

**题目描述**: 以下实例演示了多线程异常处理方法：

```java

class MyThread extends Thread{
public void run(){
System.out.println("Throwing in " +"MyThread");
        throw new RuntimeException();
    }
}
class Main {
public static void main(String[] args){
MyThread t = new MyThread();
        t.start();
        try{
Thread.sleep(1000);
        }
catch (Exception x){
System.out.println("Caught it" + x);
        }
System.out.println("Exiting main");
    }
}

```

[原文链接](https://www.runoob.com/java/exception-thread.html)
