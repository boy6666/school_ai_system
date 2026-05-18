# Java 实例 – 数组扩容

**题目描述**: 以下实例演示了如何在数组初始化后对数组进行扩容：

```java

public class Main {
public static void main(String[] args) {
String[] names = new String[] { "A", "B", "C" };
        String[] extended = new String[5];
        extended[3] = "D";
        extended[4] = "E";
        System.arraycopy(names, 0, extended, 0, names.length);
        for (String str : extended){
System.out.println(str);
        }
}
}

```

[原文链接](https://www.runoob.com/java/arrays-extension.html)
