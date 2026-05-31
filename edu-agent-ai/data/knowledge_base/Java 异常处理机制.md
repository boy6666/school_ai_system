# Java 异常处理机制

## 异常体系

```
Throwable
├── Error           — 系统级错误（OOM、StackOverflow），不应捕获
└── Exception
    ├── RuntimeException  — 非受检异常（NPE、IndexOutOfBounds、ArithmeticException）
    └── 其他 Exception     — 受检异常（IOException、SQLException），必须处理
```

## 受检 vs 非受检

| | 受检异常 (Checked) | 非受检异常 (Unchecked) |
|---|---|---|
| 父类 | Exception（非Runtime） | RuntimeException |
| 编译检查 | 必须 try-catch 或 throws | 不强制 |
| 典型 | IOException, SQLException | NullPointerException, IllegalArgumentException |

## try-catch-finally

```java
try {
    int result = 10 / 0;
} catch (ArithmeticException e) {
    System.out.println("除数不能为0：" + e.getMessage());
} catch (Exception e) {
    System.out.println("未知异常：" + e);
} finally {
    System.out.println("无论是否异常都会执行");  // 释放资源
}
```

## try-with-resources (Java 7+)

实现了 `AutoCloseable` 接口的资源会自动关闭：

```java
try (FileReader fr = new FileReader("test.txt");
     BufferedReader br = new BufferedReader(fr)) {
    String line = br.readLine();
} catch (IOException e) {
    e.printStackTrace();
}
// fr 和 br 自动调用了 close()
```

## throw vs throws

```java
// throws — 声明方法可能抛出异常
public void readFile(String path) throws IOException { ... }

// throw — 手动抛出异常
public void checkAge(int age) {
    if (age < 0) {
        throw new IllegalArgumentException("年龄不能为负数");
    }
}
```

## 自定义异常

```java
public class BusinessException extends RuntimeException {
    private int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() { return code; }
}
```

## 最佳实践

1. **不要捕获后什么都不做**（吞异常）
2. **精确捕获**：优先捕获具体异常，再捕获通用异常
3. **finally 中不要写 return**，会覆盖 try 中的返回值
4. **优先使用 try-with-resources** 管理资源
5. **自定义业务异常用 RuntimeException**，避免强制上层 try-catch
