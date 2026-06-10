# Java 实例 – 查找数组中的重复元素

**题目描述**: 以下实例演示了如何在 java 中找到重复的元素：

```java

public class MainClass {
public static void main(String[] args)
{
int[] my_array = {1, 2, 5, 5, 6, 6, 7, 2, 9, 2};
        findDupicateInArray(my_array);
 
    }
public static void findDupicateInArray(int[] a) {
int count=0;
        for(int j=0;j<a.length;j++) {
for(int k =j+1;k<a.length;k++) {
if(a[j]==a[k]) {
count++;
                }
}
if(count==1)
System.out.println( "重复元素 : " +  a[j] );
            count = 0;
        }
}
}

```

[原文链接](https://www.runoob.com/java/arrays-compare.html)
