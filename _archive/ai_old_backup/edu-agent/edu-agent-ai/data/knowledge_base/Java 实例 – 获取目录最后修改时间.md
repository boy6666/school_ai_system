# Java 实例 – 获取目录最后修改时间

**题目描述**: 以下实例演示了使用 File 类的 file.lastModified() 方法来获取目录的最后修改时间：

```java

import java.io.File;
import java.util.Date;
 
public class Main {
public static void main(String[] args) {
File file = new File("C://FileIO//demo.txt");
        System.out.println("最后修改时间：" + new Date(file.lastModified()));
    }
}

```

[原文链接](https://www.runoob.com/java/dir-modification.html)
