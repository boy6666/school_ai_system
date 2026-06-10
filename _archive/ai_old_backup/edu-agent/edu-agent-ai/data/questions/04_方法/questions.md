# 04_方法 练习题

共 7 道题目

## Q004` 为AB类的一个无形式参数无返回值的方法method书写方法头，使得使用类名（AB）作为前缀就可以调用它，该方法头的形式为？
- A. `static void method()`
- B. `public void method()`
- C. `final void method()`
- D. `abstract void method()`



`

---

## Q014` 下面关于方法覆盖的说法不正确的是？
- A. 方法覆盖要求覆盖和被覆盖的方法有相同的名字，参数列以及返回值
- B. 方法覆盖要求覆盖和被覆盖的方法必须具有相同的访问权限
- C. 覆盖的方法不能比被覆盖的方法抛出更多的异常
- D. 覆盖的方法一定不能是private的



`

---

## Q023` 假设A类有如下定义，设a是A类的一个实例，下列语句调用哪个是错误的？
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



`

---

## Q064` 在Java中，如果要让线程执行，应该调用哪个方法？
- A. run()
- B. start()
- C. execute()
- D. init()



`

---

## Q066` 以下哪个方法用于定义线程的执行体？
- A. start()
- B. init()
- C. run()
- D. synchronized()



`

---

## Q067` 下列方法中哪个是线程执行的方法？
- A. run()
- B. start()
- C. sleep()
- D. suspend()



`

---

## Q070` 关于线程状态，以下描述正确的是？
- A. 线程创建后立即进入运行状态
- B. 线程调用`start()`方法后立即进入运行状态
- C. 线程调用`sleep()`方法会进入阻塞状态
- D. 线程终止后可以再次调用`start()`重新启动



## 7️⃣ JDBC与数据库 (10题)
`

---

