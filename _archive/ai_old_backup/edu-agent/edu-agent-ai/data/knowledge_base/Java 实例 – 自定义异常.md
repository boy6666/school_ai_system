# Java 实例 – 自定义异常

**题目描述**: 以下实例演示了通过继承 Exception 来实现自定义异常：

```java

class WrongInputException extends Exception { // 自定义的类
WrongInputException(String s) {
super(s);
    }
}
class Input {
void method() throws WrongInputException {
throw new WrongInputException("Wrong input"); // 抛出自定义的类
}
}
class TestInput {
public static void main(String[] args){
try {
new Input().method();
        }
catch(WrongInputException wie) {
System.out.println(wie.getMessage());
        }
}
}

```

[原文链接](https://www.runoob.com/java/exception-user.html)
