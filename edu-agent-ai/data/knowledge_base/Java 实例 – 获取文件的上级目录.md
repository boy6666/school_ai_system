# Java 实例 – 获取文件的上级目录

**题目描述**: 以下实例演示了使用 File 类的 file.getParent()  方法来获取文件的上级目录：

```java

import java.io.File;
 
public class Main {
public static void main(String[] args) {
File file = new File("C:/File/demo.txt");
        String strParentDirectory = file.getParent();
        System.out.println("文件的上级目录为 : " + strParentDirectory);
    }
}

```

[原文链接](https://www.runoob.com/java/dir-parent.html)
