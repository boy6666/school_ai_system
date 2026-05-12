# Java 实例 – HashMap遍历

**题目描述**: 以下实例演示了如何使用 Collection 类的 iterator() 方法来遍历集合：

```java

import java.util.*;
 
public class Main {
public static void main(String[] args) {
HashMap< String, String> hMap = 
      new HashMap< String, String>();
      hMap.put("1", "1st");
      hMap.put("2", "2nd");
      hMap.put("3", "3rd");
      Collection cl = hMap.values();
      Iterator itr = cl.iterator();
      while (itr.hasNext()) {
System.out.println(itr.next());
     }
}
}

```

[原文链接](https://www.runoob.com/java/collection-iterate.html)
