# Java 实例 – 在链表（LinkedList）的开头和结尾添加元素

**题目描述**: 以下实例演示了如何使用 LinkedList 类的 addFirst() 和 addLast() 方法在链表的开头和结尾添加元素：

```java

import java.util.LinkedList;
 
public class Main {
public static void main(String[] args) {
LinkedList<String> lList = new LinkedList<String>();
        lList.add("1");
        lList.add("2");
        lList.add("3");
        lList.add("4");
        lList.add("5");
        System.out.println(lList);
        lList.addFirst("0");
        System.out.println(lList);
        lList.addLast("6");
        System.out.println(lList);
    }
}

```

[原文链接](https://www.runoob.com/java/data-insert.html)
