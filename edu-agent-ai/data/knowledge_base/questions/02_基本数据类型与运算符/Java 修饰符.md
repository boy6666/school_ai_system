**1. 以下哪个访问修饰符的可见范围最小？**
A. public
B. protected
C. default
D. private
**答案：D**
解析：private 仅在同一类内可见。

**2. 使用 protected 修饰的成员，可以被哪些类访问？**
A. 仅同一包
B. 仅子类
C. 同一包和所有子类（包括不同包）
D. 所有类
**答案：C**
解析：protected 允许同一包内任意类以及不同包的子类访问。

**3. 下列哪个修饰符可以使变量成为常量，不可修改？**
A. static
B. final
C. abstract
D. volatile
**答案：B**
解析：final 修饰变量后值不可变。

**4. 抽象方法不能使用以下哪个修饰符组合？**
A. public abstract
B. protected abstract
C. private abstract
D. default abstract
**答案：C**
解析：抽象方法需要被子类实现，private 无法被继承，因此不能组合。

**5.关于 synchronized 修饰符的说法正确的是？**
A. 保证方法或代码块的线程安全
B. 使变量变为常量
C. 禁止序列化
D. 使方法成为抽象方法
**答案：A**
解析：synchronized 用于同步，防止多线程并发问题。
