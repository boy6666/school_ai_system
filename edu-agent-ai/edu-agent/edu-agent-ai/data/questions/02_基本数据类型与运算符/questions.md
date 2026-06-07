# 02_基本数据类型与运算符 练习题

共 22 道题目

## Q001` 下列选项中，哪个关键字用于声明一个变量为常量？
- A. const
- B. final
- C. static
- D. volatile



`

---

## Q002` 下列选项中，哪个不是Java中的基本数据类型？
- A. byte
- B. double
- C. boolean
- D. object



`

---

## Q003` 以下声明合法的选项是？
- A. `default String s;`
- B. `public final static native int w();`
- C. `abstract double d;`
- D. `abstract final double hyperbolicCosine();`



`

---

## Q005` 以下哪一项是Java中合法的标识符？
- A. `Tree&Glasses`
- B. `FirstJavaApplet`
- C. `*theLastOne`
- D. `273.5`



`

---

## Q006` 下列哪个是合法的变量名？（多选）
- A. `2variable`
- B. `variable2`
- C. `_whatavariable`
- D. `_3_`
- E. `$anothervar`
- F. `#myvar`
- G. `$_￥`



`

---

## Q007` 下面程序执行后b的值是？
```
Integer integ = new Integer(9);
boolean b = integ instanceof Object;
```
- A. 9
- B. true
- C. 1
- D. false



`

---

## Q026` 下面这三条语句的输出结果分别是？
`System.out.println("is " + 100 + 5);`
`System.out.println(100 + 5 + " is");`
`System.out.println("is " + (100 + 5));`
- A. `is 1005`, `1005 is`, `is 1005`
- B. `is 105`, `105 is`, `is 105`
- C. `is 1005`, `105 is`, `is 105`
- D. `is 1005`, `1005 is`, `is 105`



`

---

## Q028` 下面哪一个循环会导致死循环？
- A. `for (int k = 0; k < 0; k++)`
- B. `for (int k = 10; k > 0; k--)`
- C. `for (int k = 0; k < 10; k--)`
- D. `for (int k = 0; k > 0; k++)`



`

---

## Q030` 关于以下程序段，正确的说法是？
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



`

---

## Q032` `int a = -2`，则表达式 `a>>>3` 的值为？
- A. 0
- B. 3
- C. 8
- D. -1



`

---

## Q033` 下面代码的输出结果是什么？
```
int x = 20, y = 5;
System.out.println(x + y + "" + (x + y) + y);
```
- A. 2530
- B. 55
- C. 2052055
- D. 25255



`

---

## Q034` `System.out.println("5" + 2);`的输出结果应该是？
- A. 52
- B. 7
- C. 2
- D. 5



`

---

## Q036` 以下哪个运算符在Java中用于逻辑非操作？
- A. !
- B. #
- C. %
- D. /



`

---

## Q037` 设有数组的定义 `int[] a = new int[3]`，则下面对数组元素的引用错误的是？
- A. `a[0];`
- B. `a[];`
- C. `a[3];`
- D. `int i=1; a[i];`



`

---

## Q038` 下列循环语句序列执行完成后，i的值是？
```
int i;
for(i=2; i<=10; i++){ }
System.out.println(i);
```
- A. 2
- B. 10
- C. 11
- D. 不确定



`

---

## Q046` 在Java中，Character流与Byte流的区别是？
- A. 每次读入的字节数不同
- B. 前者带有缓冲，后者没有
- C. 前者是字符读写，后者是字节读写
- D. 二者没有区别，可以互换使用



`

---

## Q059` 在Java类中，使用以下哪个声明语句来定义公有的int型常量MAX？
- A. `int MAX = 100;`
- B. `final int MAX = 100;`
- C. `public static int MAX = 100;`
- D. `public static final int MAX = 100;`



`

---

## Q061` 请问，以下哪些是Java中合法的标识符？（多选）
- A. `_xpoints`
- B. `r2d2`
- C. `bbb$`
- D. `set-flow`
- E. `thisiscrazy`



`

---

## Q062` 下面哪个是合法的数组声明和构造语句？
- A. `int[] ages = [100];`
- B. `int ages = new int[100];`
- C. `int[] ages = new int[100];`
- D. `int() ages = new int(100);`



`

---

## Q077` 若要在Java中表示一个空引用，应该使用什么？
- A. null
- B. 0
- C. ""
- D. false



`

---

## Q078` 下面哪一个是正确的Java注释方式？（多选）
- A. `// 这是单行注释`
- B. `/* 这是多行注释 */`
- C. `# 这是注释`
- D. `-- 这是注释`



`

---

## Q079` 下面哪个是Java中的基本数据类型？
- A. Integer
- B. String
- C. Boolean
- D. ArrayList



`

---

