# Java 实例 – List 元素替换

**题目描述**: 以下实例演示了如何使用 Collections 类的 replaceAll() 来替换List中所有的指定元素：

```java

import java.util.*;
 
public class Main {
public static void main(String[] args) {
List list = Arrays.asList("one Two three Four five six one three Four".split(" "));
      System.out.println("List :"+list);
      Collections.replaceAll(list, "one", "hundrea");
      System.out.println("replaceAll: " + list);
   }
}

```

[原文链接](https://www.runoob.com/java/collection-replace.html)
