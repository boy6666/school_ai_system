# Java 多线程编程实战

## 创建线程的 4 种方式

### 1. 继承 Thread
```java
class MyThread extends Thread {
    public void run() { System.out.println("线程运行中"); }
}
new MyThread().start();
```

### 2. 实现 Runnable（推荐）
```java
class MyTask implements Runnable {
    public void run() { System.out.println("任务执行中"); }
}
new Thread(new MyTask()).start();

// Lambda 简化
new Thread(() -> System.out.println("Lambda 线程")).start();
```

### 3. 实现 Callable（有返回值）
```java
Callable<String> task = () -> {
    Thread.sleep(1000);
    return "结果";
};
FutureTask<String> future = new FutureTask<>(task);
new Thread(future).start();
String result = future.get();  // 阻塞等待
```

### 4. 线程池（生产环境推荐）
```java
ExecutorService pool = Executors.newFixedThreadPool(5);
pool.submit(() -> System.out.println("线程池任务"));
pool.shutdown();
```

## 线程池参数（ThreadPoolExecutor）

```java
new ThreadPoolExecutor(
    2,                      // corePoolSize 核心线程数
    5,                      // maximumPoolSize 最大线程数
    60, TimeUnit.SECONDS,   // 空闲线程存活时间
    new LinkedBlockingQueue<>(100),  // 工作队列
    new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
);
```

### 常见线程池
| 工厂方法 | 特点 |
|----------|------|
| `newFixedThreadPool(n)` | 固定线程数 |
| `newCachedThreadPool()` | 无限线程，空闲回收 |
| `newSingleThreadExecutor()` | 单线程顺序执行 |
| `newScheduledThreadPool(n)` | 支持定时/周期任务 |

## synchronized 关键字

```java
// 同步方法
public synchronized void increment() { count++; }

// 同步代码块（更精细的锁粒度）
public void increment() {
    synchronized (this) { count++; }
}

// 静态方法上锁 → 锁的是 Class 对象
public static synchronized void staticMethod() { }
```

## Lock 接口

```java
ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    // 临界区代码
} finally {
    lock.unlock();  // 必须在 finally 中释放
}
```

## volatile 关键字

保证变量的**可见性**（一个线程修改后其他线程立即可见），但不保证原子性：
```java
private volatile boolean running = true;
```

## 线程通信

```java
// wait / notify（必须在 synchronized 块内使用）
synchronized (lock) {
    while (condition) { lock.wait(); }
    lock.notifyAll();
}
```

## 常见并发问题

1. **死锁**：线程 A 持有锁 1 等待锁 2，线程 B 持有锁 2 等待锁 1
2. **竞态条件**：多线程同时读写共享变量，结果取决于执行顺序
3. **ABA 问题**：CAS 操作中值从 A→B→A，使用 `AtomicStampedReference` 解决

## ConcurrentHashMap

分段锁机制，读操作无锁，写操作只锁对应段，并发性能远优于 Hashtable：

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("key", 1);
map.computeIfAbsent("key2", k -> 100);  // 原子操作
```
