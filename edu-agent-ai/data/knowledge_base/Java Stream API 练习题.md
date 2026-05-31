# Java Stream API 详解

## 什么是 Stream

Stream 是 Java 8 引入的抽象，用于对集合数据进行声明式的函数式操作。它不是数据结构，不存储数据，而是对数据源（集合、数组、I/O）进行批量处理。

## Stream 操作分为三类

1. **创建流**：`stream()`, `parallelStream()`, `Arrays.stream()`, `Stream.of()`
2. **中间操作**（惰性求值，返回 Stream）：`filter`, `map`, `sorted`, `distinct`, `limit`, `skip`, `flatMap`
3. **终结操作**（触发计算，返回非 Stream）：`collect`, `forEach`, `reduce`, `count`, `anyMatch`, `findFirst`

## 常用操作

```java
List<Student> students = Arrays.asList(
    new Student("张三", 85), new Student("李四", 92),
    new Student("王五", 73), new Student("赵六", 88)
);

// filter — 过滤
List<Student> passed = students.stream()
    .filter(s -> s.getScore() >= 80)
    .collect(Collectors.toList());

// map — 转换
List<String> names = students.stream()
    .map(Student::getName)
    .collect(Collectors.toList());

// sorted — 排序
students.stream()
    .sorted(Comparator.comparingInt(Student::getScore).reversed())
    .forEach(System.out::println);

// reduce — 聚合
int totalScore = students.stream()
    .map(Student::getScore)
    .reduce(0, Integer::sum);

// collect — 收集成 Map
Map<String, Integer> nameScoreMap = students.stream()
    .collect(Collectors.toMap(Student::getName, Student::getScore));

// 分组
Map<String, List<Student>> gradeGroups = students.stream()
    .collect(Collectors.groupingBy(s -> {
        if (s.getScore() >= 90) return "优秀";
        else if (s.getScore() >= 80) return "良好";
        else return "一般";
    }));
```

## 练习题

### Q1: 找出列表中所有偶数的平方
```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
List<Integer> result = numbers.stream()
    .filter(n -> n % 2 == 0)
    .map(n -> n * n)
    .collect(Collectors.toList());
// 输出: [4, 16, 36, 64, 100]
```

### Q2: 统计字符串列表中长度大于3的单词
```java
List<String> words = Arrays.asList("Java", "Go", "Python", "C", "Rust", "JS");
long count = words.stream()
    .filter(w -> w.length() > 3)
    .count();
// 结果: 2 (Java, Python)
```

### Q3: 将 List<List<Integer>> 扁平化为 List<Integer>
```java
List<List<Integer>> nested = Arrays.asList(
    Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5));
List<Integer> flat = nested.stream()
    .flatMap(List::stream)
    .collect(Collectors.toList());
// 输出: [1, 2, 3, 4, 5]
```

## 并行流

```java
// 数据量大时用 parallelStream
long sum = IntStream.rangeClosed(1, 10_000_000)
    .parallel()
    .sum();
```

## 注意事项

1. Stream 只能用一次，用完后不能复用
2. `parallelStream` 并非总是更快，小数据量或含 I/O 操作时不建议
3. 避免在 lambda 中修改外部变量（非 final 变量）
4. 空集合调用 stream 仍安全，返回空 Stream
