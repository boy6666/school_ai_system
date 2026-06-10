# Java 实例 – 获取目录大小

**题目描述**: 以下实例演示了使用 File 类的 FileUtils.sizeofDirectory(File Name) 来获取目录的大小：

```java

import java.io.File;
import org.apache.commons.io.FileUtils;
 
public class Main {
public static void main(String[] args) {
long size = FileUtils.sizeOfDirectory(new File("C:/test"));
        System.out.println("Size: " + size + " bytes");
    }
}

```

[原文链接](https://www.runoob.com/java/dir-size.html)
