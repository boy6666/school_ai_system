# Java 实例 – 多个异常处理（多个catch）

**题目描述**: 1，声明异常时，建议声明更为具体的异常，这样可以处理的更具体 2,对方声明几个异常，就对应几个catch块， 如果多个catch块中的异常出现继承关系，父类异常catch块放在最下面 以下实例演示了如何处理多异常：

```java

class Demo
{
int div(int a,int b) throws ArithmeticException,ArrayIndexOutOfBoundsException//在功能上通过throws的关键字声明该功能可能出现问题  
{
int []arr = new int [a];  
  
        System.out.println(arr[4]);//制造的第一处异常  
return a/b;//制造的第二处异常  
}
}
class ExceptionDemo
{
public static void main(String[]args) //throws Exception  
{
Demo d = new Demo();  
  
        try
{
int x = d.div(4,0);//程序运行截图中的三组示例 分别对应此处的三行代码  
//int x = d.div(5,0);  
//int x = d.div(4,1);  
System.out.println("x="+x);   
        }
catch (ArithmeticException e)
{
System.out.println(e.toString());  
        }
catch (ArrayIndexOutOfBoundsException e)
{
System.out.println(e.toString());  
        }
catch (Exception e)//父类 写在此处是为了捕捉其他没预料到的异常 只能写在子类异常的代码后面  
//不过一般情况下是不写的   
{
System.out.println(e.toString());  
        }
System.out.println("Over");  
    }
}

```

[原文链接](https://www.runoob.com/java/exception-hierarchy.html)
