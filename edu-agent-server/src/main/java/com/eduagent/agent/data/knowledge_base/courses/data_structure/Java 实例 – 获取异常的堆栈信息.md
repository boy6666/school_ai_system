# Java 实例 – 获取异常的堆栈信息

**题目描述**: 以下实例演示了使用异常类的 printStack()  方法来获取堆栈信息：

```java

public class Main{
public static void main (String args[]){
int array[]={20,20,40};
        int num1=15,num2=10;
        int result=10;
        try{
result = num1/num2;
            System.out.println("The result is" +result);
            for(int i =5; i>=0; i--) {
System.out.println("The value of array is" +array[i]);
            }
}
catch (Exception e) {
e.printStackTrace();
        }
}
}

```

[原文链接](https://www.runoob.com/java/exception-printstack.html)
