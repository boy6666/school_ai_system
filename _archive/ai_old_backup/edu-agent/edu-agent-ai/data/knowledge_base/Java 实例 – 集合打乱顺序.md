# Java 实例 – 集合打乱顺序

**题目描述**: 以下实例演示了如何使用 Collections 类 Collections.shuffle() 方法来打乱集合元素的顺序：

```java

import java.util.*;
 
public class Main {
public static void main(String[] args) {
List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < 10; i++)
list.add(new Integer(i));
        System.out.println("打乱前:");
        System.out.println(list);
 
        for (int i = 1; i < 6; i++) {
System.out.println("第" + i + "次打乱：");
            Collections.shuffle(list);
            System.out.println(list);
        }
}
}

```

[原文链接](https://www.runoob.com/java/collection-shuffle.html)
