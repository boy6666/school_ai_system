# Java 实例 – List 截取

**题目描述**: 以下实例演示了如何使用 Collections 类的 indexOfSubList() 和 lastIndexOfSubList() 方法来查看子列表是否在列表中，并查看子列表在列表中所在的位置：

```java

import java.util.*;
 
public class Main {
public static void main(String[] args) {
List list = Arrays.asList("one Two three Four five six one three Four".split(" "));
      System.out.println("List :"+list);
      List sublist = Arrays.asList("three Four".split(" "));
      System.out.println("子列表 :"+sublist);
      System.out.println("indexOfSubList: "
      + Collections.indexOfSubList(list, sublist));
      System.out.println("lastIndexOfSubList: "
      + Collections.lastIndexOfSubList(list, sublist));
   }
}

```

[原文链接](https://www.runoob.com/java/collection-sublist.html)
