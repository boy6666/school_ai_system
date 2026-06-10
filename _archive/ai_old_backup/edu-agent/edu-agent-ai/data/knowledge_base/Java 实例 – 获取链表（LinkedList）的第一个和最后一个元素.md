# Java 实例 – 获取链表（LinkedList）的第一个和最后一个元素

**题目描述**: 以下实例演示了如何使用 LinkedList 类的 linkedlistname.getFirst() 和 linkedlistname.getLast() 来获取链表的第一个和最后一个元素：

```java

import java.util.LinkedList;
 
public class Main {
public static void main(String[] args) {
LinkedList<String> lList = new LinkedList<String>();
        lList.add("100");
        lList.add("200");
        lList.add("300");
        lList.add("400");
        lList.add("500");
        System.out.println("链表的第一个元素是：" + lList.getFirst());
        System.out.println("链表的最后一个元素是：" + lList.getLast());
    }
}

```

[原文链接](https://www.runoob.com/java/data-element.html)
