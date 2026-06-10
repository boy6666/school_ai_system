# Java 实例 – 获取文件修改时间

**题目描述**: 以下实例演示了使用 File 类的  file.lastModified() 方法来获取文件最后的修改时间

```java

import java.io.File;
import java.util.Date;
 
public class Main {
public static void main(String[] args) {
File file = new File("Main.java");
        Long lastModified = file.lastModified();
        Date date = new Date(lastModified);
        System.out.println(date);
    }
}

```

[原文链接](https://www.runoob.com/java/file-date.html)
