# 07_类与对象 练习题

共 22 道题目

## Q010` 阅读下面代码，输出结果是什么？
```
public class Conditional{
    public static void main(String args[]){
        int x = 4;
        System.out.println("value is " + ((x > 4) ? 99.9 : 9));
    }
}
```
- A. `value is 99.9`
- B. `value is 9`
- C. `value is 9.0`
- D. 编译错误



## 2️⃣ 面向对象与类 (15题)
`

---

## Q011` 下列哪个类的声明是正确的？
- A. `abstract final class HI{}`
- B. `abstract private move(){}`
- C. `protected private number;`
- D. `public abstract class Car{}`



`

---

## Q012` 关于被私有访问控制符private修饰的成员变量，以下说法正确的是？
- A. 可以被三种类所引用：该类自身、与它在同一个包中的其他类、在其他包中的该类的子类
- B. 可以被两种类访问和引用：该类本身、该类的所有子类
- C. 只能被该类自身所访问和修改
- D. 只能被同一个包中的类访问



`

---

## Q013` 现有父类A，子类B，B的子类C，在Java源代码中有如下声明：
`1. A a0 = new A();`
`2. A a1 = new B();`
`3. A a2 = new C();`
以下哪个说法是正确的？
- A. 只有第1行能通过编译
- B. 第1、2行能通过编译，但第3行编译出错
- C. 第1、2、3行能通过编译，但第2、3行运行时出错
- D. 第1行、第2行和第3行的声明都是正确的



`

---

## Q015` 下列有关继承的说法，正确的是？
- A. 子类能继承父类的所有方法和属性
- B. 子类能继承父类的非私有方法和属性
- C. 子类只能继承父类public方法和属性
- D. 子类能继承父类的方法，而不是属性



`

---

## Q017` 在使用interface声明一个接口时，只可以使用哪个修饰符修饰该接口？
- A. private
- B. protected
- C. private protected
- D. public



`

---

## Q018` 在Java中用什么关键字修饰的方法可以直接通过类名来调用？
- A. static
- B. final
- C. private
- D. void



`

---

## Q019` 以下哪个关于抽象类的描述是错误的？
- A. abstract关键字可以修饰类或方法
- B. final类的方法都不能是abstract，因为final类不能有子类
- C. abstract类不能实例化
- D. abstract类的子类必须实现其超类的所有abstract方法



`

---

## Q021` 某类中有如下方法：`abstract void performDial();`，该方法属于？
- A. 接口方法
- B. 最终方法
- C. 抽象方法
- D. 空方法



`

---

## Q022` 以下关于继承和实现说法正确的是？
- A. 类可以实现多个接口，接口可以继承（或扩展）多个接口
- B. 类可以实现多个接口，接口不能继承（或扩展）多个接口
- C. 类和接口都可以实现多个接口
- D. 类和接口都不可以实现多个接口



`

---

## Q024` 如果想使某个变量只可以被类本身访问和调用，应使用哪种访问控制修饰符？
- A. private
- B. protected
- C. private protected
- D. public



`

---

## Q025` 下面说法不正确的是？
- A. 一个子类的对象可以接收父类对象能接收的消息
- B. 当子类对象和父类对象能接收同样的消息时，它们针对消息产生的行为可能不同
- C. 父类比它的子类的方法更多
- D. 子类在构造函数中可以使用super()来调用父类的构造函数



## 3️⃣ 控制流与运算符 (15题)
`

---

## Q039` 阅读以下代码，输出结果应该是？
```
public class Increment{
    public static void main(String args[]){
        int c = 2;
        System.out.println(c);
        System.out.println(c++);
        System.out.println(c);
    }
}
```
- A. 2 2 2
- B. 2 3 3
- C. 2 2 3
- D. 3 4 4



`

---

## Q049` 欲构造ArrayList类的一个实例，此类继承了List接口，下列哪个方法是正确的？
- A. `ArrayList myList = new Object();`
- B. `List myList = new ArrayList();`
- C. `ArrayList myList = new List();`
- D. `List myList = new List();`



`

---

## Q051` 关于下面程序，以下哪种描述是正确的？
```
public class Test {
    private static int x = 100;
    public static void main(String[] args) {
        Test hs1 = new Test();
        hs1.x++;
        Test hs2 = new Test();
        hs2.x++;
        hs1 = new Test();
        hs1.x++;
        Test.x--;
        System.out.println("x=" + x);
    }
}
```
- A. 5行不能通过编译，因为引用了私有静态变量
- B. 10行不能通过编译，因为x是私有静态变量
- C. 程序通过编译，输出结果为：x=103
- D. 程序通过编译，输出结果为：x=102



`

---

## Q060` 下面的对象创建方法中哪些会调用构造方法？（多选）
- A. new语句创建对象
- B. 调用java.io.ObjectInputStream的readObject方法
- C. java反射机制使用java.lang.Class或java.lang.reflect.Constructor的newInstance()方法
- D. 调用对象的clone()方法



`

---

## Q068` 哪个关键字可以对对象加互斥锁？
- A. transient
- B. synchronized
- C. serialize
- D. static



`

---

## Q069` 下面哪个不是Object类的方法？
- A. wait()
- B. notify()
- C. notifyAll()
- D. sleep()



`

---

## Q081` 在Java中，以下哪个访问修饰符具有最大的访问权限？
- A. private
- B. protected
- C. public
- D. default



`

---

## Q083` 要使对象具有序列化能力，则其类应该实现如下哪个接口？
- A. Serializable
- B. Cloneable
- C. Runnable
- D. Comparable



`

---

## Q085` 在Java中，用于定义类的关键字是？
- A. interface
- B. class
- C. struct
- D. object



`

---

## Q086` 下面关于`main`方法的说法正确的是？
- A. `public main(String args[])`
- B. `public static void main(String args[])`
- C. `private static void main(String args[])`
- D. `void main()`

---

