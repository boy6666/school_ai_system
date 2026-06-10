# Java 实例 – 数组获取最大和最小值

**题目描述**: 以下实例演示了如何通过 Collections 类的 Collections.max() 和 Collections.min() 方法来查找数组中的最大和最小值：

```java

import java.util.Arrays;
import java.util.Collections;
 
public class Main {
public static void main(String[] args) {
Integer[] numbers = { 8, 2, 7, 1, 4, 9, 5};
        int min = (int) Collections.min(Arrays.asList(numbers));
        int max = (int) Collections.max(Arrays.asList(numbers));
        System.out.println("最小值: " + min);
        System.out.println("最大值: " + max);
    }
}

```

[原文链接](https://www.runoob.com/java/arrays-min-max.html)
