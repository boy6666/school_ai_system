# Java 实例 – 删除链表中的元素

**题目描述**: 以下实例演示了使用 clear() 方法来删除链表中的元素：

```java

import java.util.*;
 
public class Main {
public static void main(String[] args) {
LinkedList<String> lList = new LinkedList<String>();
      lList.add("1");
      lList.add("8");
      lList.add("6");
      lList.add("4");
      lList.add("5");
      System.out.println(lList);
      lList.subList(2, 4).clear();
      System.out.println(lList);
   }
}

```

[原文链接](https://www.runoob.com/java/data-replace.html)
