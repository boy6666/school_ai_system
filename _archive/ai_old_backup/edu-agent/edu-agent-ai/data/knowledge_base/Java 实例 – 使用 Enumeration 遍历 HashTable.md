# Java 实例 – 使用 Enumeration 遍历 HashTable

**题目描述**: 以下实例演示了如何使用 Enumeration 类的 hasMoreElements 和 nextElement 方法来遍历输出 HashTable 中的内容：

```java

import java.util.Enumeration;
import java.util.Hashtable;
 
public class Main {
public static void main(String[] args) {
Hashtable ht = new Hashtable();
      ht.put("1", "One");
      ht.put("2", "Two");
      ht.put("3", "Three");
      Enumeration e = ht.elements();
      while(e.hasMoreElements()){
System.out.println(e.nextElement());
      }
}
}

```

[原文链接](https://www.runoob.com/java/collection-enumeration.html)
