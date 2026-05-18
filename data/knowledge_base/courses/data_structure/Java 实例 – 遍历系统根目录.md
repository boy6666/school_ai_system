# Java 实例 – 遍历系统根目录

**题目描述**: 以下实例演示了使用 File 类的 listRoots() 方法来输出系统所有根目录：

```java

import java.io.*;
 
class Main{
public static void main(String[] args){
File[] roots = File.listRoots();
        System.out.println("系统所有根目录：");
        for (int i=0; i < roots.length; i++) {
System.out.println(roots[i].toString());
        }
}
}

```

[原文链接](https://www.runoob.com/java/dir-root.html)
