# Java 实例 – 集合比较

**题目描述**: 以下实例将字符串转换为集合并使用 Collection 类的 Collection.min() 和 Collection.max() 来比较集合中的元素：

```java

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
 
class Main {
public static void main(String[] args) {
String[] coins = { "Penny", "nickel", "dime", "Quarter", "dollar" };
        Set<String> set = new TreeSet<String>();
        for (int i = 0; i < coins.length; i++) {
set.add(coins[i]);
        }
System.out.println(Collections.min(set));
        System.out.println(Collections.min(set, String.CASE_INSENSITIVE_ORDER));
        for (int i = 0; i <= 10; i++) {
System.out.print("-");
        }
System.out.println("");
        System.out.println(Collections.max(set));
        System.out.println(Collections.max(set, String.CASE_INSENSITIVE_ORDER));
    }
}

```

[原文链接](https://www.runoob.com/java/collection-compare.html)
