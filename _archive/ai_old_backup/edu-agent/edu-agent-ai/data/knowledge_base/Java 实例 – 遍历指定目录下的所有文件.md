# Java 实例 – 遍历指定目录下的所有文件

**题目描述**: 以下实例演示了如何使用 File 类的 list 方法来输出指定目录下的所有文件：

```java

import java.io.File;
 
class Main {
public static void main(String[] args) {
File dir = new File("C:");
        String[] children = dir.list();
        if (children == null) {
System.out.println( "目录不存在或它不是一个目录");
        }
else {
for (int i=0; i< children.length; i++) {
String filename = children[i];
                System.out.println(filename);
            }
}
}
}

```

[原文链接](https://www.runoob.com/java/dir-sub.html)
