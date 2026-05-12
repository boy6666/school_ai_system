# Java 实例 – List 循环移动元素

**题目描述**: 以下实例演示了如何使用 Collections 类的 rotate() 来循环移动元素，方法第二个参数指定了移动的起始位置：

```java

import java.util.*;
 
public class Main {
public static void main(String[] args) {
List list = Arrays.asList("one Two three Four five six".split(" "));
      System.out.println("List :"+list);
      Collections.rotate(list, 3);
      System.out.println("rotate: " + list);
   }
}

```

[原文链接](https://www.runoob.com/java/collection-rotate.html)
