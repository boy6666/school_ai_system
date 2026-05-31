# Java 集合框架入门

## 什么是集合框架

Java 集合框架（Java Collections Framework, JCF）是一组接口和类，位于 `java.util` 包中，用于存储和操作一组对象。与数组不同，集合是动态的，可以自动扩容。

## 核心接口层次

```
Collection
├── List        (有序、可重复)
│   ├── ArrayList    — 基于动态数组，查询快 O(1)，增删慢 O(n)
│   ├── LinkedList   — 基于双向链表，增删快 O(1)，查询慢 O(n)
│   └── Vector       — 线程安全的 ArrayList（已过时）
├── Set         (无序、不可重复)
│   ├── HashSet      — 基于 HashMap，O(1) 增删查
│   ├── LinkedHashSet — 维护插入顺序
│   └── TreeSet      — 基于红黑树，元素排序
└── Queue       (队列)
    ├── PriorityQueue — 优先队列（堆实现）
    └── ArrayDeque   — 双端队列，比 Stack 和 LinkedList 更快
```

## Map 接口（键值对）

```
Map
├── HashMap       — 数组+链表+红黑树，O(1)~O(log n)
├── LinkedHashMap — 维护插入/访问顺序
├── TreeMap       — 红黑树，按 key 排序
└── Hashtable     — 线程安全的 HashMap（已过时）
```

## 常用操作

```java
// List
List<String> list = new ArrayList<>();
list.add("Java"); list.get(0); list.remove(0);

// Set
Set<Integer> set = new HashSet<>();
set.add(1); set.contains(1); set.remove(1);

// Map
Map<String, Integer> map = new HashMap<>();
map.put("age", 18); map.get("age"); map.containsKey("age");

// 遍历
for (Map.Entry<String, Integer> entry : map.entrySet()) {
    System.out.println(entry.getKey() + " = " + entry.getValue());
}
```

## 如何选择集合类

| 需求 | 推荐 |
|------|------|
| 频繁查询 | ArrayList |
| 频繁增删 | LinkedList |
| 去重 | HashSet |
| 排序 | TreeSet / TreeMap |
| 键值对快速查找 | HashMap |
| 线程安全 | ConcurrentHashMap, CopyOnWriteArrayList |

## 常见面试题

1. **ArrayList vs LinkedList**：底层结构不同导致性能差异
2. **HashMap 原理**：hash 计算 → 数组索引 → 链表/红黑树
3. **HashMap 扩容机制**：默认容量 16，负载因子 0.75，2 倍扩容
4. **HashSet 如何保证不重复**：内部使用 HashMap，元素作为 key，PRESENT 作为 value
