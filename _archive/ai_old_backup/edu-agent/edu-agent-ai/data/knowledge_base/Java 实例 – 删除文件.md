# Java 实例 – 删除文件

**题目描述**: 以下实例演示了使用 delete() 方法将文件删除：

```java

import java.io.*;
 
public class Main
{
public static void main(String[] args)
{
try{
File file = new File("c:\\test.txt");
            if(file.delete()){
System.out.println(file.getName() + " 文件已被删除！");
            }else{
System.out.println("文件删除失败！");
            }
}catch(Exception e){
e.printStackTrace();
        }
}
}

```

[原文链接](https://www.runoob.com/java/file-delete.html)
