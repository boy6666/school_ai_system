# Java 实例 – 获取向量元素的索引值

**题目描述**: 以下实例演示了使用 Collections 类的 sort() 方法对向量进行排序并使用 binarySearch() 方法来获取向量元素的索引值：

```java

import java.util.Collections;
import java.util.Vector;
 
public class Main {
public static void main(String[] args) {
Vector v = new Vector();
      v.add("X");
      v.add("M");
      v.add("D");
      v.add("A");
      v.add("O");
      Collections.sort(v);
      System.out.println(v);
      int index = Collections.binarySearch(v, "D");
      System.out.println("元素索引值为 : " + index);
   }
}

```

[原文链接](https://www.runoob.com/java/data-vecsort.html)
