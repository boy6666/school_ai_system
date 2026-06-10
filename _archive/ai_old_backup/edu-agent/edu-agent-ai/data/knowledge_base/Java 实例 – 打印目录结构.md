# Java 实例 – 打印目录结构

**题目描述**: 以下实例演示了使用 File 类的 file.getName() 和 file.listFiles() 方法来打印目录结构：

```java

import java.io.File;
import java.io.IOException;
 
public class FileUtil {
public static void main(String[] a)throws IOException{
showDir(1, new File("d:\\Java"));
    }
static void showDir(int indent, File file) throws IOException {
for (int i = 0; i < indent; i++)
System.out.print('-');
        System.out.println(file.getName());
        if (file.isDirectory()) {
File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++)
showDir(indent + 4, files[i]);
        }
}
}

```

[原文链接](https://www.runoob.com/java/dir-hierarchy.html)
