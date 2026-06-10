# Java 实例 – 字符串替换

**题目描述**: 如何使用java替换字符串中的字符呢？ 以下实例中我们使用 java String 类的 replace 方法来替换字符串中的字符：

```java

public class StringReplaceEmp{
public static void main(String args[]){
String str="Hello World";
      System.out.println( str.replace( 'H','W' ) );
      System.out.println( str.replaceFirst("He", "Wa") );
      System.out.println( str.replaceAll("He", "Ha") );
   }
}

```

[原文链接](https://www.runoob.com/java/string-replace.html)
