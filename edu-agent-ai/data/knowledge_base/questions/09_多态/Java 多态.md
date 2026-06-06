1. 多态的三个必要条件是：继承、方法重写和？
   A. 方法重载
   B. 父类引用指向子类对象
   C. 抽象类
   D. 接口
   **答案：B**
2. 下列代码输出什么？

java

```
class Animal { void eat() { System.out.println("Animal eat"); } }
class Dog extends Animal { void eat() { System.out.println("Dog eat"); } }
public class Test {
    public static void main(String[] args) {
        Animal a = new Dog();
        a.eat();
    }
}
```



A. Animal eat
B. Dog eat
C. 编译错误
D. 运行时异常
**答案：B**

3.多态中，成员变量在编译和运行时访问的是？
A. 编译时看父类，运行时看父类
B. 编译时看子类，运行时看子类
C. 编译时看父类，运行时看子类
D. 编译时看子类，运行时看父类
**答案：A**

4.向下转型（downcasting）之前，通常需要使用哪个运算符判断类型？
A. is
B. instanceof
C. typeof
D. cast
**答案：B**

5.下列哪个不是多态的优点？
A. 提高代码可扩展性
B. 增加代码耦合度
C. 提高可维护性
D. 允许通用处理
**答案：B**
