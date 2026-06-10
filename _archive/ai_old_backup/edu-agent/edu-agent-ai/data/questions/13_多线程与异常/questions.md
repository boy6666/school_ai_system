# 13_多线程与异常 练习题

共 8 道题目

## Q041` 下列哪个选项是Java中用于处理异常的关键字？（多选）
- A. catch
- B. throw
- C. try
- D. finally



`

---

## Q042` 下面关键字中，哪一个不是用于异常处理语句？
- A. try
- B. break
- C. catch
- D. finally



`

---

## Q043` 假定一个方法会产生非RuntimeException异常，如果希望把异常交给调用该方法的方法处理，正确的声明方式是什么？
- A. `throw Exception`
- B. `throws Exception`
- C. `new Exception`
- D. 不需要指明什么



`

---

## Q044` 以下对异常的描述不正确的是？
- A. 异常分为Error和Exception
- B. Throwable是所有异常类的父类
- C. 在程序中无论是Error类型，还是Exception类型的异常，都可以捕获后进行异常处理
- D. Exception是RuntimeException异常的父类



`

---

## Q045` 执行下面程序的结果是什么？（其中a=4,b=0）
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



`

---

## Q063` 下面声明数组的写法错误的是？（多选）
- A. `int a[];`
- B. `int[] a;`
- C. `int[3][] a;`
- D. `int[][3] a;`



## 6️⃣ 多线程与并发 (10题)
`

---

## Q065` 在Servlet处理请求的方式为？
- A. 以进程的方式
- B. 以程序的方式
- C. 以线程的方式
- D. 以响应的方式



`

---

## Q084` 下面哪个关键字用于在Java中显式地抛出一个异常？
- A. throw
- B. exception
- C. catch
- D. try



`

---

