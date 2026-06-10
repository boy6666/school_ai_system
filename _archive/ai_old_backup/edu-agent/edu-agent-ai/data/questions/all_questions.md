---

## 1️⃣ Java基础语法与数据类型 (10题)
`## Q001` 下列选项中，哪个关键字用于声明一个变量为常量？
- A. const
- B. final
- C. static
- D. volatile

<details><summary>查看答案</summary>**正确答案：B**</details>

`## Q002` 下列选项中，哪个不是Java中的基本数据类型？
- A. byte
- B. double
- C. boolean
- D. object

<details><summary>查看答案</summary>**正确答案：D**</details>

`## Q003` 以下声明合法的选项是？
- A. `default String s;`
- B. `public final static native int w();`
- C. `abstract double d;`
- D. `abstract final double hyperbolicCosine();`

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** 在Java中，`native`修饰的方法可以没有方法体，但`abstract`方法不能`final`同时修饰。</details>

`## Q004` 为AB类的一个无形式参数无返回值的方法method书写方法头，使得使用类名（AB）作为前缀就可以调用它，该方法头的形式为？
- A. `static void method()`
- B. `public void method()`
- C. `final void method()`
- D. `abstract void method()`

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** 直接用类名调用的是静态方法，用static修饰即可。</details>

`## Q005` 以下哪一项是Java中合法的标识符？
- A. `Tree&Glasses`
- B. `FirstJavaApplet`
- C. `*theLastOne`
- D. `273.5`

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** Java标识符由字母、数字、下划线和美元符号组成，且不能以数字开头。</details>

`## Q006` 下列哪个是合法的变量名？（多选）
- A. `2variable`
- B. `variable2`
- C. `_whatavariable`
- D. `_3_`
- E. `$anothervar`
- F. `#myvar`
- G. `$_￥`

<details><summary>查看答案</summary>**正确答案：D, E, G**<br/>**解析：** 变量名不能以数字开头，不能包含`.`、空格和`#`。</details>

`## Q007` 下面程序执行后b的值是？
```
Integer integ = new Integer(9);
boolean b = integ instanceof Object;
```
- A. 9
- B. true
- C. 1
- D. false

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** 在Java中，所有类都继承自Object类，Integer对象肯定是Object的实例。</details>

`## Q008` 在Java中，`String str = "abc"`，其中的`"abc"`分配在内存的哪个区域？
- A. 堆
- B. 栈
- C. 字符串常量区
- D. 寄存器

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** 用双引号引起来的字符串字面量，存储在字符串常量池中。</details>

`## Q009` 下面哪个选项中，不属于Java语言的特点？
- A. 面向对象
- B. 高安全性
- C. 平台无关
- D. 面向过程

<details><summary>查看答案</summary>**正确答案：D**<br/>**解析：** Java是面向对象的程序设计语言。</details>

`## Q010` 阅读下面代码，输出结果是什么？
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

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** 三目运算符中，两个选项99.9（double）和9（int）的类型不同，自动取精度更高的类型，因此9被转为9.0。</details>

## 2️⃣ 面向对象与类 (15题)
`## Q011` 下列哪个类的声明是正确的？
- A. `abstract final class HI{}`
- B. `abstract private move(){}`
- C. `protected private number;`
- D. `public abstract class Car{}`

<details><summary>查看答案</summary>**正确答案：D**<br/>**解析：** abstract和final不能同时修饰一个类；修饰符不能重复。</details>

`## Q012` 关于被私有访问控制符private修饰的成员变量，以下说法正确的是？
- A. 可以被三种类所引用：该类自身、与它在同一个包中的其他类、在其他包中的该类的子类
- B. 可以被两种类访问和引用：该类本身、该类的所有子类
- C. 只能被该类自身所访问和修改
- D. 只能被同一个包中的类访问

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** private是最严格的访问权限，仅对当前类可见。</details>

`## Q013` 现有父类A，子类B，B的子类C，在Java源代码中有如下声明：
`1. A a0 = new A();`
`2. A a1 = new B();`
`3. A a2 = new C();`
以下哪个说法是正确的？
- A. 只有第1行能通过编译
- B. 第1、2行能通过编译，但第3行编译出错
- C. 第1、2、3行能通过编译，但第2、3行运行时出错
- D. 第1行、第2行和第3行的声明都是正确的

<details><summary>查看答案</summary>**正确答案：D**<br/>**解析：** 继承具有传递性，C也是A的子类，所以可以将子类的实例赋值给父类的引用。</details>

`## Q014` 下面关于方法覆盖的说法不正确的是？
- A. 方法覆盖要求覆盖和被覆盖的方法有相同的名字，参数列以及返回值
- B. 方法覆盖要求覆盖和被覆盖的方法必须具有相同的访问权限
- C. 覆盖的方法不能比被覆盖的方法抛出更多的异常
- D. 覆盖的方法一定不能是private的

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** 子类覆盖方法的访问权限可以比父类更宽松，但不能更严格。</details>

`## Q015` 下列有关继承的说法，正确的是？
- A. 子类能继承父类的所有方法和属性
- B. 子类能继承父类的非私有方法和属性
- C. 子类只能继承父类public方法和属性
- D. 子类能继承父类的方法，而不是属性

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** 子类会继承父类所有非私有的属性和方法。</details>

`## Q016` 下面哪个关键字用于在Java中定义一个接口？
- A. interface
- B. abstract
- C. class
- D. method

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** `interface`关键字用于定义接口。</details>

`## Q017` 在使用interface声明一个接口时，只可以使用哪个修饰符修饰该接口？
- A. private
- B. protected
- C. private protected
- D. public

<details><summary>查看答案</summary>**正确答案：D**<br/>**解析：** 接口可以是public的，也可以是默认的（default），但不能是private或protected。</details>

`## Q018` 在Java中用什么关键字修饰的方法可以直接通过类名来调用？
- A. static
- B. final
- C. private
- D. void

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** 静态方法（static修饰）属于类本身，可以直接通过类名调用。</details>

`## Q019` 以下哪个关于抽象类的描述是错误的？
- A. abstract关键字可以修饰类或方法
- B. final类的方法都不能是abstract，因为final类不能有子类
- C. abstract类不能实例化
- D. abstract类的子类必须实现其超类的所有abstract方法

<details><summary>查看答案</summary>**正确答案：D**<br/>**解析：** 如果抽象类的子类也是抽象的，可以不实现父类的抽象方法。</details>

`## Q020` `String str1 = "abc"`，`String str2 = new String("abc")`，那么`str1 == str2`的结果是？
- A. true
- B. false

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** `==`比较的是对象的引用地址，`str1`指向字符串常量池，`str2`指向堆内存中的对象，二者地址不同。</details>

`## Q021` 某类中有如下方法：`abstract void performDial();`，该方法属于？
- A. 接口方法
- B. 最终方法
- C. 抽象方法
- D. 空方法

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** 由`abstract`修饰且没有方法体的方法是抽象方法。</details>

`## Q022` 以下关于继承和实现说法正确的是？
- A. 类可以实现多个接口，接口可以继承（或扩展）多个接口
- B. 类可以实现多个接口，接口不能继承（或扩展）多个接口
- C. 类和接口都可以实现多个接口
- D. 类和接口都不可以实现多个接口

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** 类可以`implements`多个接口，接口可以`extends`多个接口。</details>

`## Q023` 假设A类有如下定义，设a是A类的一个实例，下列语句调用哪个是错误的？
```
class A {
    int i;
    static String s;
    void method1() { }
    static void method2() { }
}
```
- A. `System.out.println(a.i);`
- B. `a.method1();`
- C. `A.method1();`
- D. `A.method2();`

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** `method1()`是实例方法，必须通过对象来调用，不能直接通过类名调用。</details>

`## Q024` 如果想使某个变量只可以被类本身访问和调用，应使用哪种访问控制修饰符？
- A. private
- B. protected
- C. private protected
- D. public

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** 只有`private`修饰的成员只能在本类中被访问。</details>

`## Q025` 下面说法不正确的是？
- A. 一个子类的对象可以接收父类对象能接收的消息
- B. 当子类对象和父类对象能接收同样的消息时，它们针对消息产生的行为可能不同
- C. 父类比它的子类的方法更多
- D. 子类在构造函数中可以使用super()来调用父类的构造函数

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** 子类在继承父类方法的同时，还可以扩展自己的方法，因此子类的方法通常比父类更多。</details>

## 3️⃣ 控制流与运算符 (15题)
`## Q026` 下面这三条语句的输出结果分别是？
`System.out.println("is " + 100 + 5);`
`System.out.println(100 + 5 + " is");`
`System.out.println("is " + (100 + 5));`
- A. `is 1005`, `1005 is`, `is 1005`
- B. `is 105`, `105 is`, `is 105`
- C. `is 1005`, `105 is`, `is 105`
- D. `is 1005`, `1005 is`, `is 105`

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** 字符串拼接时，只要遇到字符串，整个表达式就转为字符串操作。但括号内会优先进行算术运算。</details>

`## Q027` 下面关于for循环和while循环的说法中哪个是正确的？
- A. while循环能实现的操作，for循环也都能实现
- B. while循环判断条件一般是程序结果，for循环判断条件一般是非程序结果
- C. 两种循环任何时候都可替换
- D. 两种循环结构中都必须有循环体，循环体不能为空

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** for循环和while循环在功能上是等价的，可以相互转换。</details>

`## Q028` 下面哪一个循环会导致死循环？
- A. `for (int k = 0; k < 0; k++)`
- B. `for (int k = 10; k > 0; k--)`
- C. `for (int k = 0; k < 10; k--)`
- D. `for (int k = 0; k > 0; k++)`

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** 变量k从0开始，每次减1，永远小于10，循环条件始终为true，造成死循环。</details>

`## Q029` 执行如下程序段后，total的值为？
```
int total = 0;
for (int i = 0; i < 4; i++){
    if (i == 1) continue;
    if (i == 2) break;
    total += i;
}
```
- A. 0
- B. 1
- C. 3
- D. 6

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** i=0时，total加0；i=1时，continue跳出本次循环；i=2时，break终止整个循环，因此total始终为0。</details>

`## Q030` 关于以下程序段，正确的说法是？
`String s1 = "a" + "b";`
`String s2 = new String(s1);`
`if (s1 == s2)`
`    System.out.println("== is succeeded");`
`if (s1.equals(s2))`
`    System.out.println(".equals() is succeeded");`
- A. 第4行与第6行都将执行
- B. 第4行执行，第6行不执行
- C. 第6行执行，第4行不执行
- D. 第4行、第6行都不执行

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** `s1`来自常量池，`s2`通过new在堆中创建，`==`比较地址为false；`equals()`比较内容为true。</details>

`## Q031` `Math.round(-2.5)` 的结果是多少？
- A. -2
- B. -3
- C. -2.5
- D. -3.5

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** `round()`函数在数轴上是向右取整，`-2.5`向右取整为`-2`。</details>

`## Q032` `int a = -2`，则表达式 `a>>>3` 的值为？
- A. 0
- B. 3
- C. 8
- D. -1

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** `>>>`是无符号右移，无论正负，高位都补0。`-2`的二进制右移3位后变为全0，结果是0。</details>

`## Q033` 下面代码的输出结果是什么？
```
int x = 20, y = 5;
System.out.println(x + y + "" + (x + y) + y);
```
- A. 2530
- B. 55
- C. 2052055
- D. 25255

<details><summary>查看答案</summary>**正确答案：D**<br/>**解析：** 先计算括号内的算术运算，遇到字符串后，后面的加号都变成字符串拼接。</details>

`## Q034` `System.out.println("5" + 2);`的输出结果应该是？
- A. 52
- B. 7
- C. 2
- D. 5

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** 字符串在前，加号被解释为字符串连接符，将数字2转为字符串拼接到后面。</details>

`## Q035` 下列关于`for`循环和`while`循环的说法中哪个是正确的？
- A. `while`循环能实现的操作，`for`循环也都能实现
- B. `while`循环判断条件一般是程序结果，`for`循环判断条件一般是非程序结果
- C. 两种循环任何时候都可替换
- D. 两种循环结构中都必须有循环体，循环体不能为空

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** `for`循环是`while`循环的增强形式，功能上完全等价，可以相互转换。</details>

`## Q036` 以下哪个运算符在Java中用于逻辑非操作？
- A. !
- B. #
- C. %
- D. /

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** `!`是逻辑非运算符，用于取反布尔值。</details>

`## Q037` 设有数组的定义 `int[] a = new int[3]`，则下面对数组元素的引用错误的是？
- A. `a[0];`
- B. `a[];`
- C. `a[3];`
- D. `int i=1; a[i];`

<details><summary>查看答案</summary>**正确答案：B, C**<br/>**解析：** `a[]`是错误语法；`a[3]`下标越界，最大索引是2。</details>

`## Q038` 下列循环语句序列执行完成后，i的值是？
```
int i;
for(i=2; i<=10; i++){ }
System.out.println(i);
```
- A. 2
- B. 10
- C. 11
- D. 不确定

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** 循环条件`i<=10`，当`i=11`时循环结束，此时i的值为11。</details>

`## Q039` 阅读以下代码，输出结果应该是？
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

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** `c++`是后置自增，先输出当前值2，再自增为3。</details>

`## Q040` 下列哪一个关键字用于终止当前循环，并开始下一次循环迭代？
- A. break
- B. continue
- C. return
- D. exit

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** `continue`用于跳过本次循环的剩余部分，立即开始下一次迭代。</details>

## 4️⃣ 异常处理与I/O (15题)
`## Q041` 下列哪个选项是Java中用于处理异常的关键字？（多选）
- A. catch
- B. throw
- C. try
- D. finally

<details><summary>查看答案</summary>**正确答案：A, C, D**<br/>**解析：** `try`、`catch`、`finally`是异常处理的关键字，`throw`和`throws`用于抛出异常。</details>

`## Q042` 下面关键字中，哪一个不是用于异常处理语句？
- A. try
- B. break
- C. catch
- D. finally

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** `break`用于跳出循环或switch语句，不属于异常处理关键字。</details>

`## Q043` 假定一个方法会产生非RuntimeException异常，如果希望把异常交给调用该方法的方法处理，正确的声明方式是什么？
- A. `throw Exception`
- B. `throws Exception`
- C. `new Exception`
- D. 不需要指明什么

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** `throws`用在方法声明后面，表示该方法可能抛出的异常，由调用者处理。</details>

`## Q044` 以下对异常的描述不正确的是？
- A. 异常分为Error和Exception
- B. Throwable是所有异常类的父类
- C. 在程序中无论是Error类型，还是Exception类型的异常，都可以捕获后进行异常处理
- D. Exception是RuntimeException异常的父类

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** Error通常是系统级错误，程序无法处理，也不应该捕获。</details>

`## Q045` 执行下面程序的结果是什么？（其中a=4,b=0）
```
public static void divide(int a, int b) {
    try { int c = a / b; }
    catch (Exception e) { System.out.print("Exception "); }
    finally { System.out.print("Finally"); }
}
```
- A. `Exception Finally`
- B. `Finally`
- C. `Exception`
- D. 没有输出结果

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** 除零异常触发`catch`块，且无论是否异常`finally`块都会执行。</details>

`## Q046` 在Java中，Character流与Byte流的区别是？
- A. 每次读入的字节数不同
- B. 前者带有缓冲，后者没有
- C. 前者是字符读写，后者是字节读写
- D. 二者没有区别，可以互换使用

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** 字节流以字节为单位，字符流以字符为单位，处理文本文件时更推荐字符流。</details>

`## Q047` 要从文件"file.dat"中读出第10个字节到变量c中，下列哪个方法适合？
- A. `FileInputStream in=new FileInputStream("file.dat"); in.skip(9); int c=in.read();`
- B. `FileInputStream in=new FileInputStream("file.dat"); in.skip(10); int c=in.read();`
- C. `FileInputStream in=new FileInputStream("file.dat"); int c=in.read();`
- D. `RandomAccessFile in=new RandomAccessFile("file.dat"); in.skip(9); int c=in.readByte();`

<details><summary>查看答案</summary>**正确答案：D**<br/>**解析：** `RandomAccessFile`可以跳过9个字节后读取第10个字节。`FileInputStream`顺序读取，不适合。</details>

`## Q048` Java编程所必须的默认引用包为？
- A. `java.sys`
- B. `java.lang`
- C. `java.util`
- D. 以上都不是

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** `java.lang`包是Java语言的核心包，使用前不需要`import`。</details>

## 5️⃣ 集合框架与泛型 (15题)
`## Q049` 欲构造ArrayList类的一个实例，此类继承了List接口，下列哪个方法是正确的？
- A. `ArrayList myList = new Object();`
- B. `List myList = new ArrayList();`
- C. `ArrayList myList = new List();`
- D. `List myList = new List();`

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** ArrayList是List接口的实现类，可以通过父类引用指向子类对象，但List是接口不能实例化。</details>

`## Q050` 下列哪个集合类实现了队列的接口？
- A. ArrayList
- B. LinkedList
- C. HashSet
- D. HashMap

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** LinkedList实现了Queue接口，可以用作队列数据结构。</details>

`## Q051` 关于下面程序，以下哪种描述是正确的？
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

<details><summary>查看答案</summary>**正确答案：D**<br/>**解析：** 虽然x是private，但在类内部可以访问。静态变量属于类，所有实例共享同一个x。</details>

`## Q052` `ArrayList list = new ArrayList(20);`中的list扩充了几次？
- A. 0
- B. 1
- C. 2
- D. 3

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** 当指定初始容量时，ArrayList会直接分配相应大小的数组，不会立即发生扩容。</details>

`## Q053` 下面哪些接口直接继承自Collection接口？（多选）
- A. List
- B. Map
- C. Set
- D. Iterator

<details><summary>查看答案</summary>**正确答案：A, C**<br/>**解析：** Map是独立的接口，Iterator不是Collection的子接口。</details>

`## Q054` 关于List和Set的区别，说法正确的是？
- A. List元素有放入顺序，元素可重复；Set元素无放入顺序，元素不可重复
- B. List元素无放入顺序，元素可重复；Set元素有放入顺序，元素不可重复
- C. List和Set都有放入顺序，元素都可重复
- D. List和Set都无放入顺序，元素都不可重复

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** List有序可重复，Set无序不可重复。</details>

`## Q055` 下面选项中哪一个是正确的创建Map集合的方式？
- A. `Map m = new Map();`
- B. `Map m = new Map(init capacity, increment capacity);`
- C. `Map m = new Map(new Collection());`
- D. Map是接口，不能实例化。

<details><summary>查看答案</summary>**正确答案：D**<br/>**解析：** Map是接口，需要通过实现类如HashMap来实例化。</details>

`## Q056` 以下哪个接口用于实现迭代器功能？
- A. Iterable
- B. Iterator
- C. Collection
- D. List

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** Iterator接口定义了hasNext()和next()等方法，用于遍历集合。</details>

`## Q057` 关于HashSet的说法正确的是？
- A. HashSet继承自AbstractSet
- B. HashSet继承自HashMap
- C. HashSet继承自Collection
- D. HashSet继承自List

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** HashSet继承自AbstractSet，同时实现了Set接口。</details>

`## Q058` 在Java中，使用JDBC时，对于多次调用同一条SQL语句的情况，使用哪个通常会提高效率？
- A. Statement
- B. CallableStatement
- C. PreparedStatement
- D. SQLStatement

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** PreparedStatement会预编译SQL语句，多次执行时效率更高。</details>

`## Q059` 在Java类中，使用以下哪个声明语句来定义公有的int型常量MAX？
- A. `int MAX = 100;`
- B. `final int MAX = 100;`
- C. `public static int MAX = 100;`
- D. `public static final int MAX = 100;`

<details><summary>查看答案</summary>**正确答案：D**<br/>**解析：** 常量需同时用`public static final`修饰，变量名通常全大写。</details>

`## Q060` 下面的对象创建方法中哪些会调用构造方法？（多选）
- A. new语句创建对象
- B. 调用java.io.ObjectInputStream的readObject方法
- C. java反射机制使用java.lang.Class或java.lang.reflect.Constructor的newInstance()方法
- D. 调用对象的clone()方法

<details><summary>查看答案</summary>**正确答案：A, C**<br/>**解析：** 序列化的`readObject()`和`clone()`方法均不会调用构造器。</details>

`## Q061` 请问，以下哪些是Java中合法的标识符？（多选）
- A. `_xpoints`
- B. `r2d2`
- C. `bbb$`
- D. `set-flow`
- E. `thisiscrazy`

<details><summary>查看答案</summary>**正确答案：A, B, C, E**<br/>**解析：** 标识符只能由字母、数字、下划线和美元符号组成，且不能以数字开头，不能包含连接符。</details>

`## Q062` 下面哪个是合法的数组声明和构造语句？
- A. `int[] ages = [100];`
- B. `int ages = new int[100];`
- C. `int[] ages = new int[100];`
- D. `int() ages = new int(100);`

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** Java中标准的数组声明方式是`type[] varName = new type[size];`。</details>

`## Q063` 下面声明数组的写法错误的是？（多选）
- A. `int a[];`
- B. `int[] a;`
- C. `int[3][] a;`
- D. `int[][3] a;`

<details><summary>查看答案</summary>**正确答案：C, D**<br/>**解析：** Java中声明数组时，不能在方括号内指定大小，大小是在创建对象时指定的。</details>

## 6️⃣ 多线程与并发 (10题)
`## Q064` 在Java中，如果要让线程执行，应该调用哪个方法？
- A. run()
- B. start()
- C. execute()
- D. init()

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** `start()`会启动新线程，新线程执行`run()`方法；直接调用`run()`只是普通方法调用。</details>

`## Q065` 在Servlet处理请求的方式为？
- A. 以进程的方式
- B. 以程序的方式
- C. 以线程的方式
- D. 以响应的方式

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** Servlet容器为每个请求创建一个线程来处理，而非进程。</details>

`## Q066` 以下哪个方法用于定义线程的执行体？
- A. start()
- B. init()
- C. run()
- D. synchronized()

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** `run()`方法中包含线程要执行的代码，是线程的执行体。</details>

`## Q067` 下列方法中哪个是线程执行的方法？
- A. run()
- B. start()
- C. sleep()
- D. suspend()

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** `run()`是线程的入口，包含线程执行的代码。</details>

`## Q068` 哪个关键字可以对对象加互斥锁？
- A. transient
- B. synchronized
- C. serialize
- D. static

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** `synchronized`用于线程同步，对对象或代码块加锁。</details>

`## Q069` 下面哪个不是Object类的方法？
- A. wait()
- B. notify()
- C. notifyAll()
- D. sleep()

<details><summary>查看答案</summary>**正确答案：D**<br/>**解析：** `sleep()`是Thread类的静态方法，属于线程操作，不在Object类中。</details>

`## Q070` 关于线程状态，以下描述正确的是？
- A. 线程创建后立即进入运行状态
- B. 线程调用`start()`方法后立即进入运行状态
- C. 线程调用`sleep()`方法会进入阻塞状态
- D. 线程终止后可以再次调用`start()`重新启动

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** `sleep()`使线程进入阻塞状态；`start()`使线程进入就绪态，等待CPU调度；终止的线程不能重新启动。</details>

## 7️⃣ JDBC与数据库 (10题)
`## Q071` JDBC中，用于表示数据库连接的对象是？
- A. Statement
- B. Connection
- C. DriverManager
- D. PreparedStatement

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** `Connection`代表与特定数据库的连接会话。</details>

`## Q072` 用于调用存储过程的对象是？
- A. ResultSet
- B. DriverManager
- C. CallableStatement
- D. PreparedStatement

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** `CallableStatement`用于执行数据库中的存储过程。</details>

`## Q073` 获取ResultSet对象rst的下一行数据，以下正确的是？
- A. `rst.hasNext()`
- B. `rst.next()`
- C. `rst.first()`
- D. `rst.nextRow()`

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** JDBC中ResultSet的`next()`方法用于将光标从当前位置移到下一行。</details>

`## Q074` ResultSet常用定位方法boolean next()的作用是？
- A. 定位到指定编号的记录上
- B. 定位到第一行
- C. 定位到最后一行
- D. 从前往后移动一行

<details><summary>查看答案</summary>**正确答案：D**<br/>**解析：** `next()`将光标从当前位置向下移动一行，如果没有下一行则返回false。</details>

`## Q075` 如果数据库中某个字段为numeric型，可以通过结果集中的哪个方法获取？
- A. `getNumberic()`
- B. `getDouble()`
- C. `setNumberic()`
- D. `setDouble()`

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** JDBC中numeric类型通常可以通过`getDouble()`或`getBigDecimal()`获取。</details>

`## Q076` 在Java中开发JDBC应用程序时，使用DriverManager类的getConnection()方法建立与数据源的连接语句为：`Connection con = DriverManager.getConnection("jdbc:microsoft:sqlserver://localhost:1433;DatabaseName=news");`。URL连接中的“news”表示的是？
- A. 用户名
- B. 数据库的名称
- C. 数据库服务器的机器名
- D. 数据库中表的名称

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** JDBC URL中DatabaseName参数指定要连接的数据库实例。</details>

## 8️⃣ 其他核心知识点 (10题)
`## Q077` 若要在Java中表示一个空引用，应该使用什么？
- A. null
- B. 0
- C. ""
- D. false

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** `null`是Java中专门表示引用不指向任何对象的字面量。</details>

`## Q078` 下面哪一个是正确的Java注释方式？（多选）
- A. `// 这是单行注释`
- B. `/* 这是多行注释 */`
- C. `# 这是注释`
- D. `-- 这是注释`

<details><summary>查看答案</summary>**正确答案：A, B**<br/>**解析：** Java支持`//`和`/* */`两种注释方式，不支持`#`。</details>

`## Q079` 下面哪个是Java中的基本数据类型？
- A. Integer
- B. String
- C. Boolean
- D. ArrayList

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** Java基本数据类型包括byte、short、int、long、float、double、char、boolean。Boolean是包装类。</details>

`## Q080` 在Java中，如何将一个字符串转换为整数？
- A. `Integer.parseInt()`
- B. `String.toInt()`
- C. `(int)String`
- D. `int.valueOf(String)`

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** `Integer.parseInt()`是将字符串转换为int类型的最标准方法。</details>

`## Q081` 在Java中，以下哪个访问修饰符具有最大的访问权限？
- A. private
- B. protected
- C. public
- D. default

<details><summary>查看答案</summary>**正确答案：C**<br/>**解析：** 访问权限从大到小为：public > protected > default > private。</details>

`## Q082` 关于`break`和`continue`的说法，正确的是？
- A. `break`跳出当前循环，`continue`结束本次循环继续下一次
- B. `break`结束本次循环继续下一次，`continue`跳出当前循环
- C. `break`和`continue`可以互相替换
- D. `break`和`continue`只能用在`for`循环中

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** `break`终止循环，`continue`跳过本次循环的剩余部分，继续下一次迭代。</details>

`## Q083` 要使对象具有序列化能力，则其类应该实现如下哪个接口？
- A. Serializable
- B. Cloneable
- C. Runnable
- D. Comparable

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** 实现`java.io.Serializable`接口可使对象被序列化。</details>

`## Q084` 下面哪个关键字用于在Java中显式地抛出一个异常？
- A. throw
- B. exception
- C. catch
- D. try

<details><summary>查看答案</summary>**正确答案：A**<br/>**解析：** `throw`用于在代码中显式抛出一个异常对象。</details>

`## Q085` 在Java中，用于定义类的关键字是？
- A. interface
- B. class
- C. struct
- D. object

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** `class`关键字用于定义类。</details>

`## Q086` 下面关于`main`方法的说法正确的是？
- A. `public main(String args[])`
- B. `public static void main(String args[])`
- C. `private static void main(String args[])`
- D. `void main()`

<details><summary>查看答案</summary>**正确答案：B**<br/>**解析：** `main`方法的正确签名是`public static void main(String[] args)`。</details>

