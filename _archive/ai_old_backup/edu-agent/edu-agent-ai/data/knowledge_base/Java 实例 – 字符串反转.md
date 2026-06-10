# Java 实例 – 字符串反转

**题目描述**: 以下实例演示了如何使用 Java 的反转函数 reverse() 将字符串反转：

```java

public class StringReverseExample{
public static void main(String[] args){
String string="runoob";
      String reverse = new StringBuffer(string).reverse().toString();
      System.out.println("字符串反转前:"+string);
      System.out.println("字符串反转后:"+reverse);
   }
}

```

[原文链接](https://www.runoob.com/java/string-reverse.html)
