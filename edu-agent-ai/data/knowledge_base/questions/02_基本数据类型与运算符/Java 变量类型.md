**1.在 Java 中，声明在方法内部的变量称为？**
A. 成员变量
B. 局部变量
C. 静态变量
D. 实例变量
**答案：B**
解析：局部变量在方法、构造器或块内声明。

**2. 以下关于实例变量的描述，正确的是？**
A. 使用 static 修饰
B. 属于类，所有实例共享一份
C. 在对象创建时创建，销毁时销毁
D. 必须显式初始化，否则编译错误
**答案：C**
解析：实例变量属于对象实例，随对象创建和销毁；有默认值，不必显式初始化。

**3. 静态变量（类变量）使用哪个关键字修饰？**
A. final
B. static
C. private
D. volatile
**答案：B**
解析：static 关键字声明类变量，所有实例共享。

**4. 以下代码的输出结果是什么？**

java

```
public class Test {
    static int count = 0;
    public Test() { count++; }
    public static void main(String[] args) {
        new Test(); new Test();
        System.out.println(Test.count);
    }
}
```



A. 0
B. 1
C. 2
D. 编译错误
**答案：C**
解析：静态变量 count 每创建一个对象增加 1，创建两个对象后值为 2。

**5. 局部变量在使用前必须？**
A. 声明为 public
B. 初始化
C. 声明为 static
D. 声明为 final
**答案：B**
解析：局部变量没有默认值，使用前必须显式赋值。
