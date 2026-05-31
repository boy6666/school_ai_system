# Java 面试高频题精讲

## 1. == 和 equals 的区别

- `==` 比较的是**引用地址**（基本类型比较值）
- `equals()` 默认也是比较引用地址，但 String、Integer 等重写了 equals 方法，比较**内容**

```java
String a = new String("hello");
String b = new String("hello");
System.out.println(a == b);      // false（不同对象）
System.out.println(a.equals(b)); // true（内容相同）
```

## 2. String、StringBuilder、StringBuffer

| | String | StringBuilder | StringBuffer |
|---|---|---|---|
| 可变性 | 不可变（final） | 可变 | 可变 |
| 线程安全 | 安全（不可变） | 不安全 | 安全（synchronized） |
| 性能 | 拼接产生新对象 | 快 | 较慢 |

- 少量拼接用 `+`（编译器优化为 StringBuilder）
- 循环拼接用 StringBuilder
- 多线程用 StringBuffer

## 3. HashMap 底层原理

**JDK 1.8+**：数组 + 链表 + 红黑树

1. 计算 key 的 hashCode → hash → 定位数组索引
2. 该位置为空 → 直接放入
3. 该位置有元素 → 链表法处理冲突（尾插法）
4. 链表长度 ≥ 8 且数组长度 ≥ 64 → 转红黑树（提升查询效率）
5. 扩容：容量 × 2，重新 hash 分配

```java
// 核心参数
static final int DEFAULT_INITIAL_CAPACITY = 16;
static final float DEFAULT_LOAD_FACTOR = 0.75f;
static final int TREEIFY_THRESHOLD = 8;
```

## 4. JVM 内存模型

```
堆 (Heap)：存放对象实例，GC 主要区域
├── 新生代 (Young Gen)：Eden + Survivor×2
├── 老年代 (Old Gen)
└── 永久代/元空间 (PermGen → Metaspace, JDK 8+)

栈 (Stack)：线程私有，存储局部变量、方法调用帧
方法区 (Method Area)：类信息、常量、静态变量
本地方法栈 (Native Stack)：native 方法
程序计数器 (PC Register)：当前线程执行位置
```

## 5. 垃圾回收算法

| 算法 | 描述 | 优缺点 |
|------|------|--------|
| 标记-清除 | 标记活对象，清除未标记 | 产生内存碎片 |
| 复制算法 | 将存活对象复制到另一区域 | 空间利用率 50% |
| 标记-整理 | 标记后向一端移动整理 | 无碎片，但耗时 |

**分代回收**：新生代用复制算法（对象朝生夕死），老年代用标记-整理/清除

## 6. Spring 核心概念

### IOC（控制反转）
将对象创建和依赖关系的控制权交给 Spring 容器，通过 DI（依赖注入）实现：

```java
@Autowired  // 自动注入
private UserService userService;

@Bean      // 注册 Bean
public DataSource dataSource() { ... }
```

### AOP（面向切面编程）
将横切关注点（日志、事务、权限）从业务代码中抽离：

```java
@Aspect
@Component  // 非 AOP 注解，确保 Spring 扫描到
public class LogAspect {
    @Before("execution(* com.example.service.*.*(..))")
    public void before(JoinPoint jp) {
        System.out.println("调用: " + jp.getSignature().getName());
    }
}
```

### Bean 生命周期
实例化 → 属性注入 → Aware 接口回调 → BeanPostProcessor 前置处理 → @PostConstruct 初始化 → BeanPostProcessor 后置处理 → 就绪 → @PreDestroy 销毁

## 7. 设计模式高频题

### 单例模式（双重检查锁）
```java
public class Singleton {
    private static volatile Singleton instance;
    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```
> 要点：双重检查 + volatile 禁止指令重排

### 工厂模式
```java
interface Animal { void speak(); }
class Dog implements Animal { public void speak() { System.out.println("汪汪"); } }
class Cat implements Animal { public void speak() { System.out.println("喵喵"); } }

class AnimalFactory {
    public static Animal create(String type) {
        return switch (type) {
            case "dog" -> new Dog();
            case "cat" -> new Cat();
            default -> throw new IllegalArgumentException("未知类型");
        };
    }
}
```
