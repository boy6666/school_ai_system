# Java 实例 – 链表修改

**题目描述**: 以下实例演示了使用 listname.add() 和 listname.set() 方法来修改链接中的元素：

```java

import java.util.LinkedList;
 
public class Main {
public static void main(String[] a) {
LinkedList officers = new LinkedList();
      officers.add("B");
      officers.add("B");
      officers.add("T");
      officers.add("H");
      officers.add("P");
      System.out.println(officers);
      officers.set(2, "M");
      System.out.println(officers);
   }
}

```

[原文链接](https://www.runoob.com/java/data-update.html)
