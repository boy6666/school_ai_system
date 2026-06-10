# Java 实例 – 判断文件是否隐藏

**题目描述**: 以下实例演示了使用 File 类的 file.isHidden() 方法来判断文件是否隐藏：

```java

import java.io.File;
 
public class Main {
public static void main(String[] args) {
File file = new File("C:/Demo.txt");
        System.out.println(file.isHidden());
    }
}

```

[原文链接](https://www.runoob.com/java/dir-hidden.html)
