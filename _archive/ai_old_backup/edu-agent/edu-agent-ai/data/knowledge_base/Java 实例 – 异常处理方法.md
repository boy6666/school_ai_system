# Java 实例 – 异常处理方法

**题目描述**: 以下实例演示了使用 System 类的 System.err.println() 来展示异常的处理方法：

```java

class ExceptionDemo
{
public static void main(String[] args) {
try {
throw new Exception("My Exception");
        } catch (Exception e) {
System.err.println("Caught Exception");
            System.err.println("getMessage():" + e.getMessage());
            System.err.println("getLocalizedMessage():" + e.getLocalizedMessage());
            System.err.println("toString():" + e);
            System.err.println("printStackTrace():");
            e.printStackTrace();
        }
}
}

```

[原文链接](https://www.runoob.com/java/exception-method.html)
