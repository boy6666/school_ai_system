# Java 实例 – 判断目录是否为空

**题目描述**: 以下实例演示了使用 File 类的file.isDirectory()和file.list()方法来判断目录是否为空：

```java

import java.io.File;
 
public class Main
{
public static void main(String[] args)
{
File file = new File("./testdir");  // 当前目录下的 testdir目录
if(file.isDirectory()){
if(file.list().length>0){
System.out.println("目录不为空!");
            }else{
System.out.println("目录为空!");
            }
}else{
System.out.println("这不是一个目录!");
        }
}
}

```

[原文链接](https://www.runoob.com/java/dir-empty.html)
