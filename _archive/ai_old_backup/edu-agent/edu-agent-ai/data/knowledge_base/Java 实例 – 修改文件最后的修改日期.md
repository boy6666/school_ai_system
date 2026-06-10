# Java 实例 – 修改文件最后的修改日期

**题目描述**: 以下实例演示了使用 File 类的 fileToChange.lastModified() 和 fileToChange setLastModified() 方法来修改文件最后的修改日期：

```java

import java.io.File;
import java.util.Date;
 
public class Main {
public static void main(String[] args) throws Exception {
File fileToChange = new File("C:/myjavafile.txt");
        fileToChange.createNewFile();
        Date filetime = new Date(fileToChange.lastModified());
        System.out.println(filetime.toString());
        System.out.println(fileToChange.setLastModified(System.currentTimeMillis()));
        filetime = new Date(fileToChange.lastModified());
        System.out.println(filetime.toString());
    }
}

```

[原文链接](https://www.runoob.com/java/file-date-modify.html)
