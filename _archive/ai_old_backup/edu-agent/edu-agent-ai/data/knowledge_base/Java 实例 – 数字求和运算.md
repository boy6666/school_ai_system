# Java 实例 – 数字求和运算

**题目描述**: 以下实例演示了使用do...while结构求0~100的整数数字之和：

```java

public class Main {
public static void main(String[] args) {
int limit=100;
        int sum=0;
        int i=1;
        do
{
sum=sum+i;
            i++;
        }
while(i<=limit);
        System.out.println("sum="+sum);
    }
}

```

[原文链接](https://www.runoob.com/java/data-add.html)
