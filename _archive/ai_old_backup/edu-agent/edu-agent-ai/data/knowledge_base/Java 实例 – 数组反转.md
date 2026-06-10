# Java 实例 – 数组反转

**题目描述**: 以下实例中我们使用自定义的 reverse 方法将数组进行反转：

```java

public class RunoobTest { 
  
    /* 反转数组*/
    static void reverse(int a[], int n) 
    { 
        int[] b = new int[n]; 
        int j = n; 
        for (int i = 0; i < n; i++) { 
            b[j - 1] = a[i]; 
            j = j - 1; 
        } 
  
        /*输入反转数组*/
        System.out.println("反转后数组是: \n"); 
        for (int k = 0; k < n; k++) { 
            System.out.println(b[k]); 
        } 
    } 
  
    public static void main(String[] args) 
    { 
        int [] arr = {10, 20, 30, 40, 50}; 
        reverse(arr, arr.length); 
    } 
} 

```

[原文链接](https://www.runoob.com/java/arrays-reverse.html)
