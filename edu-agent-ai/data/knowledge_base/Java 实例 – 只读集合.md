# Java 实例 – 只读集合

**题目描述**: 以下实例演示了如何使用 Collection 类的 Collections.unmodifiableList() 方法来设置集合为只读：

```java

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
 
public class Main {
public static void main(String[] argv)
throws Exception {
List stuff = Arrays.asList(new String[] { "a", "b" });
      List list = new ArrayList(stuff);
      list = Collections.unmodifiableList(list);
      try {
list.set(0, "new value");
      }
catch (UnsupportedOperationException e) {
}
Set set = new HashSet(stuff);
      set = Collections.unmodifiableSet(set);
      Map map = new HashMap();
      map = Collections.unmodifiableMap(map);
      System.out.println("集合现在是只读");
   }
}

```

[原文链接](https://www.runoob.com/java/collection-readonly.html)
