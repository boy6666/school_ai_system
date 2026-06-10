# Java 实例 – 阶乘

**题目描述**: 一个正整数的阶乘（英语：factorial）是所有小于及等于该数的正整数的积，并且有0的阶乘为1。自然数n的阶乘写作n!。 亦即n!=1×2×3×...×n。阶乘亦可以递归方式定义：0!=1，n!=(n-1)!×n。 以下实例演示了 Java 阶乘代码的实现：

```java

public class MainClass {
public static void main(String args[]) {
for (int counter = 0; counter <= 10; counter++){
System.out.printf("%d! = %d\n", counter,
        factorial(counter));
    }
}
public static long factorial(long number) {
if (number <= 1)
return 1;
        else
return number * factorial(number - 1);
    }
}

```

[原文链接](https://www.runoob.com/java/method-factorial.html)
