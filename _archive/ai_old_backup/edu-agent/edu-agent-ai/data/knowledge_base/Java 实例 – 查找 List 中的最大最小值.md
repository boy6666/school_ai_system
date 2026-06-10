# Java 实例 – 查找 List 中的最大最小值

**题目描述**: 以下实例演示了如何使用 Collections 类的 max() 和 min() 方法来获取List中最大最小值：

```java

import java.util.*;
 
public class Main {
public static void main(String[] args) {
List list = Arrays.asList("one Two three Four five six one three Four".split(" "));
      System.out.println(list);
      System.out.println("最大值: " + Collections.max(list));
      System.out.println("最小值: " + Collections.min(list));
   }
}

```

[原文链接](https://www.runoob.com/java/collection-minmax.html)
