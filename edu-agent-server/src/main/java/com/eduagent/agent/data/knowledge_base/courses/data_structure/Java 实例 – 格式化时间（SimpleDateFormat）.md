# Java 实例 – 格式化时间（SimpleDateFormat）

**题目描述**: 以下实例演示了如何使用 SimpleDateFormat 类的 format(date) 方法来格式化时间

```java

import java.text.SimpleDateFormat;
import java.util.Date;
 
public class Main{
public static void main(String[] args){
Date date = new Date();
        String strDateFormat = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        System.out.println(sdf.format(date));
    }
}

```

[原文链接](https://www.runoob.com/java/date-time-am-pm.html)
