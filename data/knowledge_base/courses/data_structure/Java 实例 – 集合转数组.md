# Java 实例 – 集合转数组

**题目描述**: 以下实例演示了如何使用 Java Util 类的 list.add() 和 list.toArray() 方法将集合转为数组：

```java

import java.util.*;
 
public class Main{
public static void main(String[] args){
List<String> list = new ArrayList<String>();
      list.add("菜"); 
      list.add("鸟"); 
      list.add("教");
      list.add("程");
      list.add("www.runoob.com");
      String[] s1 = list.toArray(new String[0]); 
      for(int i = 0; i < s1.length; ++i){
String contents = s1[i];
         System.out.print(contents);
     }
}
}

```

[原文链接](https://www.runoob.com/java/collection-conversion.html)
