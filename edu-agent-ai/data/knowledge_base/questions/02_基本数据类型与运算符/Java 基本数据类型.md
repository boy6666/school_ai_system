**1. Java 中 char 类型占用的位数是？**
A. 8 位
B. 16 位
C. 32 位
D. 64 位
**答案：B**
解析：char 是 16 位 Unicode 字符。

**2. 下列哪个是 Java 的浮点型默认类型？**
A. float
B. double
C. long
D. int
**答案：B**
解析：小数常量默认为 double 类型。

**3. 下列代码输出结果正确的是？**

java

```
System.out.println(Byte.MIN_VALUE);
```



A. -128
B. -127
C. 0
D. 255
**答案：A**
解析：byte 范围 -128 到 127。

**4. 强制类型转换 int 到 byte，当 int=128 时，byte 的值是？**
A. 128
B. -128
C. 0
D. 编译错误
**答案：B**
解析：128 超出 byte 范围（-128~127），发生溢出，结果为 -128。

**5. 以下字面量中，哪个代表 double 类型？**
A. 123
B. 123L
C. 123.0
D. 123F
**答案：C**
解析：123.0 默认是 double；123F 是 float；123L 是 long。
