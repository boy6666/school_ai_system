# Java 实例 – 链试异常

**题目描述**: 以下实例演示了使用多个 catch 来处理链试异常：

```java

public class Main {
public static void main (String args[])throws Exception {
int n=20,result=0;
        try{
result=n/0;
            System.out.println("结果为"+result);
        }
catch(ArithmeticException ex){
System.out.println("发算术异常: "+ex);
            try {
throw new NumberFormatException();
            }
catch(NumberFormatException ex1) {
System.out.println("手动抛出链试异常 : "+ex1);
            }
}
}
}

```

[原文链接](https://www.runoob.com/java/exception-chain.html)
