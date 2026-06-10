# Java 实例 – 删除数组元素

**题目描述**: Java 的数组是固定长度的，无法直接删除，我们可以通过创建一个新数组，把原始数组中要保留的元素放到新数组中即可： 我们也可以使用 ArrayList 来实现这个功能，ArrayList 是动态数组，操作起来更加方便。 以下实例演示了如何使用 ArrayList 的 remove () 方法来删除数组列表的元素：

```java

import java.util.Arrays;
 
public class RunoobTest {
public static void main(String[] args) {
int[] oldarray = new int[] {3, 4, 5, 6, 7};// 原始数组
int num = 2;   // 删除索引为 2 的元素，即删除第三个元素 5
int[] newArray = new int[oldarray.length-1];// 新数组，长度为原始数组减去 1
for(int i=0;i<newArray.length; i++) {
// 判断元素是否越界
if (num < 0 || num >= oldarray.length) {
throw new RuntimeException("元素越界... "); 
            }
// 
if(i<num) {
newArray[i] = oldarray[i];
            }
else {
newArray[i] = oldarray[i+1];
            }
}
// 打印输出数组内容
System.out.println(Arrays.toString(oldarray));
        oldarray = newArray;
        System.out.println(Arrays.toString(oldarray));
    }
}

```

[原文链接](https://www.runoob.com/java/arrays-remove.html)
