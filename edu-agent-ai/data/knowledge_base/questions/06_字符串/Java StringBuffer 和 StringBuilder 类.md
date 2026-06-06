1. 以下哪个类是线程安全的？
   A. String
   B. StringBuilder
   C. StringBuffer
   D. 以上都是
   **答案：C**
2. 一般情况下，单线程字符串拼接推荐使用？
   A. String
   B. StringBuffer
   C. StringBuilder
   D. 字符数组
   **答案：C**
3. 下列哪个方法可以在指定位置插入字符串？
   A. append()
   B. insert()
   C. setCharAt()
   D. replace()
   **答案：B**
4. 将 StringBuffer 对象转为 String 应该使用？
   A. toString()
   B. toCharArray()
   C. getString()
   D. valueOf()
   **答案：A**
5. 以下代码输出什么？

java

```
StringBuilder sb = new StringBuilder("abc");
sb.reverse();
System.out.println(sb);
```



A. abc
B. cba
C. 编译错误
D. 运行时异常
**答案：B**
