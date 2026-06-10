# Java 实例 – 获取向量的最大元素

**题目描述**: 以下实例演示了使用 Vector 类的 v.add() 方法及 Collection 类的 Collections.max() 来获取向量的最大元素：

```java

import java.util.Collections;
import java.util.Vector;
 
public class Main {
public static void main(String[] args) {
Vector v = new Vector();
      v.add(new Double("3.4324"));
      v.add(new Double("3.3532"));
      v.add(new Double("3.342"));
      v.add(new Double("3.349"));
      v.add(new Double("2.3"));
      Object obj = Collections.max(v);
      System.out.println("最大元素是："+obj);
   }
}

```

[原文链接](https://www.runoob.com/java/data-vec-max.html)
