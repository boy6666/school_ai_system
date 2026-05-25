-- EduAgent 演示数据
-- 作者: Ocean
-- 创建时间: 2026-05-20
-- 更新时间: 2026-05-22 (替换为Java知识点体系)
-- 描述: 包含用户、资源、任务、练习题等演示数据

USE `edu_agent`;

-- ================================
-- 1. 用户数据
-- ================================

-- 插入管理员用户
INSERT INTO `users` (`username`, `password`, `nickname`, `email`, `role`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin@edu-agent.com', 'admin', 'active');

-- 插入教师用户
INSERT INTO `users` (`username`, `password`, `nickname`, `email`, `role`, `status`) VALUES
('teacher001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张教授', 'zhang@edu-agent.com', 'teacher', 'active'),
('teacher002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李老师', 'li@edu-agent.com', 'teacher', 'active');

-- 插入学生用户
INSERT INTO `users` (`username`, `password`, `nickname`, `email`, `phone`, `role`, `status`, `last_login_time`, `last_login_ip`) VALUES
('student001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', 'zhangsan@edu-agent.com', '13800138001', 'student', 'active', NOW() - INTERVAL 1 HOUR, '192.168.1.100'),
('student002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', 'lisi@edu-agent.com', '13800138002', 'student', 'active', NOW() - INTERVAL 2 DAY, '192.168.1.101'),
('student003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王五', 'wangwu@edu-agent.com', '13800138003', 'student', 'active', NOW() - INTERVAL 3 DAY, '192.168.1.102'),
('student004', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵六', 'zhaoliu@edu-agent.com', '13800138004', 'student', 'active', NOW() - INTERVAL 1 WEEK, '192.168.1.103'),
('student005', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '钱七', 'qianqi@edu-agent.com', '13800138005', 'student', 'inactive', NOW() - INTERVAL 1 MONTH, '192.168.1.104');

-- ================================
-- 2. 角色数据
-- ================================

INSERT INTO `roles` (`role_name`, `role_desc`, `permissions`, `status`) VALUES
('超级管理员', '系统最高权限，可以管理所有功能', '["user:manage", "resource:manage", "task:manage", "practice:manage", "report:manage", "settings:manage", "logs:view"]', 'active'),
('教师', '可以管理资源、练习题、查看学习报告', '["resource:manage", "practice:manage", "report:view"]', 'active'),
('学生', '可以使用学习资源、完成练习、查看报告', '["resource:view", "practice:do", "task:manage", "report:view"]', 'active');

-- ================================
-- 3. 学生画像数据
-- ================================

INSERT INTO `student_profiles` (`student_id`, `major`, `grade`, `course`, `topic`, `learning_goal`, `knowledge_base`, `cognitive_style`, `pace`, `weaknesses`, `mistake_patterns`, `resource_preference`, `overall_type`, `profile_suggestions`, `last_score`) VALUES
(4, '计算机科学与技术', '大二', 'Java 面向对象编程', '多态与接口', '期末考 85 分以上', '掌握Java基本语法', '偏好图解、代码案例和练习题', '中速', '["多态", "接口回调"]', '["概念混淆", "向上转型与向下转型易错"]', '["讲解文档", "思维导图", "练习题", "代码案例"]', '稳定提升型', '["建议围绕多态和接口进行查漏补缺。", "建议增加变式练习，提升知识迁移能力。", "建议定期复盘错题，形成稳定的解题方法。"]', 75),
(5, '软件工程', '大一', 'Java 程序设计基础', '类与对象', '掌握并能应用当前知识点', '基础薄弱，刚接触编程', '偏好结构化讲解和动画演示', '慢速', '["类与对象", "方法定义"]', '["概念混淆", "语法错误频繁"]', '["讲解文档", "动画演示", "基础练习", "代码案例"]', '基础补齐型', '["建议先补充前置知识，降低学习难度。", "建议采用分步骤讲解，每次只聚焦一个知识点。", "建议多使用图解、代码示例和基础练习。"]', 45),
(6, '信息管理与信息系统', '大二', 'Java 集合与泛型', 'HashMap原理', '掌握并能应用当前知识点', '有一定Java基础', '偏好结构化讲解', '中速', '["HashMap", "泛型"]', '["概念混淆", "源码理解不足"]', '["讲解文档", "思维导图", "练习题", "代码案例"]', '基础补齐型', '["建议先补充前置知识，降低学习难度。", "建议采用分步骤讲解，每次只聚焦一个知识点。", "建议多使用图解、代码示例和基础练习。"]', 50),
(7, '计算机科学与技术', '大三', 'Java 8 新特性', 'Stream API', '掌握并能应用当前知识点', '熟悉Java面向对象和集合', '偏好实战项目', '快速', '["Stream操作", "Lambda表达式"]', '["语法混淆", "方法引用不熟练"]', '["讲解文档", "思维导图", "练习题", "代码案例"]', '进阶拓展型', '["建议围绕薄弱点进行查漏补缺。", "建议增加变式练习，提升知识迁移能力。", "建议定期复盘错题，形成稳定的解题方法。"]', 70);

-- ================================
-- 4. 课程数据
-- ================================

INSERT INTO `courses` (`id`, `course_name`, `course_code`, `description`, `teacher_id`, `duration`, `difficulty`, `tags`, `status`) VALUES
('java_basics', 'Java 程序设计基础', 'CS101', '学习Java基本语法、流程控制、数组和基础编程思想', 2, '10周', '入门', '["Java", "基础", "编程入门"]', 'published'),
('java_oop', 'Java 面向对象编程', 'CS201', '深入理解封装、继承、多态、抽象类、接口等OOP核心概念', 2, '12周', '基础', '["Java", "面向对象", "OOP"]', 'published'),
('java_collections', 'Java 集合与泛型', 'CS301', '掌握List、Set、Map等集合框架及泛型编程', 3, '8周', '进阶', '["Java", "集合", "泛型"]', 'published'),
('java_features', 'Java 8 新特性', 'CS401', '学习Lambda表达式、Stream API、Optional等Java 8核心新特性', 2, '6周', '进阶', '["Java", "Stream", "Lambda"]', 'published');

-- ================================
-- 5. 学习资源数据
-- ================================

INSERT INTO `resources` (`title`, `type`, `difficulty`, `description`, `content`, `file_url`, `file_size`, `cover`, `author`, `rating`, `views`, `duration`, `course_id`, `course_name`, `chapter_name`, `tags`, `goals`, `suitable_for`, `status`, `teacher_id`) VALUES
('Java 面向对象编程详解', '文档', '基础', '详细讲解Java封装、继承、多态三大特性', '# Java 面向对象编程详解\n\n## 封装\n\n封装是将数据和操作数据的方法绑定在一起，对外隐藏实现细节。\n\n### 访问修饰符\n\n| 修饰符 | 同类 | 同包 | 子类 | 全局 |\n|--------|------|------|------|------|\n| private | ✓ | ✗ | ✗ | ✗ |\n| default | ✓ | ✓ | ✗ | ✗ |\n| protected | ✓ | ✓ | ✓ | ✗ |\n| public | ✓ | ✓ | ✓ | ✓ |\n\n## 继承\n\n```java\npublic class Animal {\n    protected String name;\n    \n    public void eat() {\n        System.out.println(name + " is eating");\n    }\n}\n\npublic class Dog extends Animal {\n    public void bark() {\n        System.out.println(name + " is barking");\n    }\n}\n```\n\n## 多态\n\n多态是指同一个方法调用，在不同对象上表现出不同的行为。', '/uploads/resources/java_oop_basic.pdf', '3.5MB', '/uploads/covers/java_oop.jpg', '张教授', 4.6, 1800, '60分钟', 'java_oop', 'Java 面向对象编程', '封装、继承与多态', '["Java", "面向对象", "OOP"]', '["理解封装的意义和实现方式", "掌握继承的使用场景", "能够运用多态编写灵活代码"]', '["有Java基础语法的学员", "需要系统学习OOP的学员"]', 'published', 2),

('Java 集合框架入门', '视频', '进阶', '系统讲解List、Set、Map三大集合接口', NULL, '/uploads/resources/java_collections.mp4', '220MB', '/uploads/covers/java_collections.jpg', '张教授', 4.9, 3200, '90分钟', 'java_collections', 'Java 集合与泛型', '集合框架概述', '["Java", "集合", "List", "Set", "Map"]', '["掌握List接口及常用实现类", "理解Set的去重机制", "掌握HashMap的工作原理"]', '["有一定Java基础的学员", "准备面试的学员"]', 'published', 2),

('Java 基础语法PPT', 'PPT', '入门', 'Java基础语法完整课件，涵盖变量、运算符、流程控制', NULL, '/uploads/resources/java_basics.pptx', '18MB', '/uploads/covers/java_basics.jpg', '张教授', 4.3, 950, '未知', 'java_basics', 'Java 程序设计基础', '课程总览', '["Java", "基础语法", "课件"]', '["了解Java语言特点", "掌握基本语法规则", "能够编写简单Java程序"]', '["零基础学员", "编程初学者"]', 'published', 2),

('Java 异常处理机制', '动画', '入门', '直观展示try-catch-finally的执行流程和异常传播', NULL, '/uploads/resources/exception_animation.mp4', '12MB', '/uploads/covers/java_exception.gif', '李老师', 4.5, 2100, '15分钟', 'java_basics', 'Java 程序设计基础', '异常处理', '["Java", "异常", "try-catch"]', '["理解异常的分类体系", "掌握try-catch-finally用法", "了解throws和throw的区别"]', '["初学者", "可视化学习者"]', 'published', 3),

('Java Stream API 练习题', '题库', '进阶', 'Lambda表达式与Stream API专项练习，含答案解析', NULL, '/uploads/resources/stream_exercises.pdf', '2.8MB', '/uploads/covers/java_stream.jpg', '张教授', 4.7, 1600, '未知', 'java_features', 'Java 8 新特性', 'Stream API', '["Java", "Stream", "Lambda", "练习"]', '["掌握常用Stream中间操作", "理解Stream的惰性求值", "能够用Stream替代传统循环"]', '["有Java基础的学习者", "准备升级到Java 8的开发者"]', 'published', 2),

('ArrayList 源码解析', '代码案例', '基础', '手写简化版ArrayList，理解动态数组底层实现', '```java\npublic class MyArrayList<E> {\n    private Object[] elementData;\n    private int size;\n    private static final int DEFAULT_CAPACITY = 10;\n\n    public MyArrayList() {\n        elementData = new Object[DEFAULT_CAPACITY];\n    }\n\n    public void add(E e) {\n        if (size == elementData.length) {\n            grow();\n        }\n        elementData[size++] = e;\n    }\n\n    private void grow() {\n        int newCapacity = elementData.length + (elementData.length >> 1);\n        elementData = Arrays.copyOf(elementData, newCapacity);\n    }\n\n    @SuppressWarnings("unchecked")\n    public E get(int index) {\n        checkIndex(index);\n        return (E) elementData[index];\n    }\n\n    private void checkIndex(int index) {\n        if (index < 0 || index >= size) {\n            throw new IndexOutOfBoundsException();\n        }\n    }\n\n    public int size() {\n        return size;\n    }\n}\n```', NULL, '1.5KB', '/uploads/covers/arraylist_code.jpg', '李老师', 4.4, 1200, '未知', 'java_collections', 'Java 集合与泛型', 'List 接口', '["Java", "ArrayList", "集合", "源码"]', '["理解ArrayList的动态扩容机制", "能够自己实现简易ArrayList", "掌握泛型在集合中的应用"]', '["Java学习者", "实践型学习者"]', 'published', 3),

('Java 多线程编程实战', '实验项目', '高级', '通过生产者-消费者模式实战，掌握线程创建与同步', NULL, '/uploads/resources/thread_experiment.zip', '68KB', '/uploads/covers/java_thread.jpg', '李老师', 4.2, 580, '3小时', 'java_features', 'Java 8 新特性', '并发编程', '["Java", "多线程", "并发"]', '["掌握Thread和Runnable创建线程", "理解synchronized同步机制", "能够使用线程池管理线程"]', '["有OOP基础的学员", "进阶学习者"]', 'published', 3),

('Java 面试高频题精讲', '拓展阅读', '高级', '涵盖集合、多线程、JVM等常见面试考点', NULL, '/uploads/resources/java_interview.pdf', '12MB', '/uploads/covers/java_interview.jpg', '张教授', 4.8, 720, '未知', 'java_collections', 'Java 集合与泛型', '综合提升', '["Java", "面试", "集合", "多线程"]', '["熟悉Java常见面试考点", "深入理解集合与并发原理", "提升Java综合能力"]', '["准备面试的学员", "有一定经验的开发者"]', 'published', 2);

-- ================================
-- 6. 资源章节数据
-- ================================

INSERT INTO `resource_chapters` (`resource_id`, `title`, `description`, `duration`, `content`, `order_num`) VALUES
(1, '封装与访问控制', '理解封装的意义和访问修饰符的使用', '15分钟', '# 封装\n\n封装是面向对象三大特性之一，通过private修饰符隐藏内部状态。', 1),
(1, '继承与super关键字', '继承的实现方式和super的用法', '20分钟', '# 继承\n\nJava通过extends关键字实现单继承，子类可以复用父类的属性和方法。', 2),
(1, '多态与动态绑定', '多态的实现原理和应用场景', '25分钟', '# 多态\n\n```java\nAnimal a = new Dog();\na.eat(); // 调用的是Dog的eat方法\n```', 3),
(2, 'List接口与ArrayList', 'List接口的常用方法及ArrayList实现', '30分钟', '# List接口\n\nList是有序集合，允许重复元素。ArrayList基于动态数组实现。', 1),
(2, 'Set接口与HashSet', 'Set的去重机制和HashSet原理', '30分钟', '# Set接口\n\nSet不允许重复元素。HashSet基于HashMap实现，依赖hashCode和equals方法。', 2),
(2, 'Map接口与HashMap', 'HashMap的底层结构和put/get流程', '30分钟', '# HashMap原理\n\nJDK 8中HashMap采用数组+链表+红黑树结构，负载因子默认0.75。', 3);

-- ================================
-- 7. 学习任务数据
-- ================================

INSERT INTO `learning_tasks` (`user_id`, `title`, `description`, `course_name`, `chapter_name`, `start_time`, `end_time`, `priority`, `status`, `progress`) VALUES
(4, '完成多态特性学习', '深入理解多态的实现原理和应用场景', 'Java 面向对象编程', '封装、继承与多态', NOW() - INTERVAL 2 HOUR, NOW() + INTERVAL 4 HOUR, 'high', 'doing', 60),
(4, '学习接口与抽象类', '掌握接口和抽象类的区别与使用', 'Java 面向对象编程', '抽象类与接口', NOW(), NOW() + INTERVAL 1 DAY, 'high', 'todo', 0),
(5, '类与对象入门', '学习Java中类和对象的基本概念', 'Java 程序设计基础', '类与对象', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 3 DAY, 'middle', 'doing', 30),
(6, 'HashMap源码理解', '深入理解HashMap的底层实现机制', 'Java 集合与泛型', '集合框架概述', NOW() - INTERVAL 3 DAY, NOW() + INTERVAL 1 DAY, 'low', 'done', 100),
(7, 'Stream API 实战', '用Stream API替代传统循环编写代码', 'Java 8 新特性', 'Stream API', NOW() - INTERVAL 1 WEEK, NOW() + INTERVAL 1 WEEK, 'middle', 'todo', 0);

-- ================================
-- 8. 练习题数据
-- ================================

INSERT INTO `practice_questions` (`type`, `difficulty`, `topic`, `course_id`, `chapter`, `question`, `options`, `answer`, `analysis`, `tags`, `score`, `status`, `teacher_id`) VALUES
('选择题', '基础', '面向对象', 'java_oop', '封装、继承与多态', '下列关于Java多态的说法，正确的是？', '["多态只在运行时确定方法调用", "多态就是方法重载", "多态只适用于接口", "多态需要在编译时确定类型"]', '多态只在运行时确定方法调用', 'Java多态通过动态绑定在运行时确定实际调用的方法，这是多态的核心特征。方法重载属于编译时多态，但通常说的多态指运行时多态。', '["Java", "多态", "面向对象"]', 10, 'published', 2),

('判断题', '基础', '面向对象', 'java_oop', '封装、继承与多态', 'Java中一个类可以同时继承多个父类。', NULL, '错误', 'Java只支持单继承，一个类只能有一个直接父类，但可以通过实现多个接口来弥补。', '["Java", "继承"]', 5, 'published', 2),

('填空题', '基础', '面向对象', 'java_oop', '封装、继承与多态', 'Java中使用______关键字来声明接口。', NULL, 'interface', 'interface是Java中声明接口的关键字，接口中的方法默认是public abstract的。', '["Java", "接口", "语法"]', 5, 'published', 2),

('简答题', '进阶', '面向对象', 'java_oop', '封装、继承与多态', '简述Java中抽象类和接口的区别。', NULL, '1. 抽象类可以有构造方法，接口不能有\n2. 抽象类可以有普通成员变量，接口中的变量默认是public static final\n3. 一个类只能继承一个抽象类，但可以实现多个接口\n4. 抽象类可以有具体方法，接口在Java 8后可以有default方法', '抽象类和接口是Java实现抽象化的两种机制，各有适用场景。抽象类适合"is-a"关系且需要共享代码的场景，接口适合"can-do"的契约定义。', '["Java", "抽象类", "接口"]', 15, 'published', 2),

('代码题', '高级', '面向对象', 'java_oop', '封装、继承与多态', '编写一个简单的多态示例：Animal父类和Dog、Cat子类，每个类有makeSound方法，通过父类引用调用不同子类的方法。', NULL, '```java\nabstract class Animal {\n    abstract void makeSound();\n}\n\nclass Dog extends Animal {\n    void makeSound() {\n        System.out.println("汪汪");\n    }\n}\n\nclass Cat extends Animal {\n    void makeSound() {\n        System.out.println("喵喵");\n    }\n}\n\npublic class Test {\n    public static void main(String[] args) {\n        Animal a1 = new Dog();\n        Animal a2 = new Cat();\n        a1.makeSound(); // 汪汪\n        a2.makeSound(); // 喵喵\n    }\n}\n```', '这是多态的经典示例，通过父类引用指向不同子类对象，调用同一方法产生不同行为。', '["Java", "多态", "代码"]', 20, 'published', 2),

('选择题', '进阶', '集合', 'java_collections', '集合框架概述', 'HashMap在JDK 8中当链表长度超过多少时会转为红黑树？', '["6", "7", "8", "10"]', '8', '当链表长度超过8且数组长度大于等于64时，链表会转换为红黑树，以提高查询效率从O(n)到O(log n)。', '["Java", "HashMap", "集合"]', 10, 'published', 2),

('选择题', '基础', '集合', 'java_collections', 'List 接口', 'ArrayList和LinkedList的主要区别是什么？', '["底层数据结构不同：数组 vs 双向链表", "ArrayList线程安全，LinkedList不是", "LinkedList不允许null元素", "ArrayList查询慢，LinkedList查询快"]', '底层数据结构不同：数组 vs 双向链表', 'ArrayList基于动态数组，查询O(1)，插入删除O(n)；LinkedList基于双向链表，查询O(n)，头尾插入删除O(1)。两者都非线程安全。', '["Java", "ArrayList", "LinkedList"]', 10, 'published', 3),

('判断题', '基础', '集合', 'java_collections', '集合框架概述', 'HashSet保证元素的插入顺序。', NULL, '错误', 'HashSet不保证元素的顺序。如果需要有序，应该使用LinkedHashSet（按插入顺序）或TreeSet（按自然顺序排序）。', '["Java", "HashSet", "集合"]', 5, 'published', 3);

-- ================================
-- 9. 练习记录数据
-- ================================

INSERT INTO `practice_records` (`user_id`, `question_id`, `user_answer`, `is_correct`, `score`, `time_spent`, `practice_time`) VALUES
(4, 1, '多态只在运行时确定方法调用', 1, 10, 20, NOW() - INTERVAL 1 HOUR),
(4, 2, '正确', 0, 0, 8, NOW() - INTERVAL 1 HOUR),
(4, 3, 'interface', 1, 5, 10, NOW() - INTERVAL 1 HOUR),
(4, 4, '1. 抽象类可以有构造方法...', 1, 15, 180, NOW() - INTERVAL 50 MINUTE),
(5, 7, '底层数据结构不同：数组 vs 双向链表', 1, 10, 25, NOW() - INTERVAL 30 MINUTE),
(5, 8, '正确', 0, 0, 12, NOW() - INTERVAL 25 MINUTE),
(6, 6, '8', 1, 10, 30, NOW() - INTERVAL 3 DAY);

-- ================================
-- 10. 辅导会话数据
-- ================================

INSERT INTO `tutor_sessions` (`user_id`, `session_id`, `title`, `topic`, `status`) VALUES
(4, 'session_001', 'Java多态学习辅导', '多态', 'active'),
(5, 'session_002', '类与对象基础问答', '类与对象', 'active'),
(7, 'session_003', 'Stream与Lambda讨论', 'Stream API', 'ended');

-- ================================
-- 11. 辅导消息数据
-- ================================

INSERT INTO `tutor_messages` (`session_id`, `user_id`, `role`, `content`, `message_time`) VALUES
('session_001', 4, 'user', '多态的动态绑定是怎么实现的？', NOW() - INTERVAL 1 HOUR),
('session_001', 4, 'assistant', 'Java的动态绑定通过方法表实现。每个类在JVM中都有一个方法表，记录着该类所有方法的入口地址。当通过父类引用调用方法时，JVM会根据对象的实际类型去查找对应的方法表，找到实际要调用的方法。这就是运行时多态的底层机制。', NOW() - INTERVAL 1 HOUR + INTERVAL 5 SECOND),
('session_001', 4, 'user', '那向上转型和向下转型呢？', NOW() - INTERVAL 1 HOUR + INTERVAL 2 MINUTE),
('session_001', 4, 'assistant', '向上转型是自动的，比如 `Animal a = new Dog()`，把子类对象赋值给父类引用。向下转型需要强制转换，比如 `Dog d = (Dog) a`，但前提是这个引用实际指向的就是Dog对象，否则会抛出ClassCastException。可以用instanceof先判断。', NOW() - INTERVAL 1 HOUR + INTERVAL 2 MINUTE + INTERVAL 5 SECOND),
('session_002', 5, 'user', 'Java中类和对象到底是什么关系？', NOW() - INTERVAL 30 MINUTE),
('session_002', 5, 'assistant', '类是对象的模板或蓝图，对象是类的实例。打个比方：\n1. 类就像建筑图纸，对象就是按图纸盖出来的房子\n2. 一个类可以创建多个对象，每个对象都有自己的属性值\n3. 类定义了对象有什么属性（成员变量）和能做什么（方法）', NOW() - INTERVAL 30 MINUTE + INTERVAL 10 SECOND);

-- ================================
-- 12. 辅导建议数据
-- ================================

INSERT INTO `tutor_suggestions` (`session_id`, `title`, `prompt`, `order_num`) VALUES
('session_001', '多态基础概念', '请详细讲解Java多态的概念和实现原理', 1),
('session_001', '多态练习题', '给我一些Java多态相关的练习题', 2),
('session_001', '多态实战应用', '多态在实际项目开发中的应用场景', 3),
('session_002', '类与对象入门', '请用通俗的方式讲解类和对象', 1),
('session_002', '构造方法', 'Java中构造方法的作用和用法', 2);

-- ================================
-- 13. 学习记录数据
-- ================================

INSERT INTO `learning_records` (`user_id`, `resource_id`, `action`, `duration`, `progress`, `record_time`) VALUES
(4, 1, 'view', 300, 0, NOW() - INTERVAL 1 DAY),
(4, 1, 'start', 0, 10, NOW() - INTERVAL 1 DAY + INTERVAL 1 SECOND),
(4, 1, 'progress', 900, 60, NOW() - INTERVAL 23 HOUR),
(4, 2, 'view', 1200, 0, NOW() - INTERVAL 12 HOUR),
(4, 2, 'start', 0, 20, NOW() - INTERVAL 12 HOUR + INTERVAL 1 SECOND),
(5, 5, 'view', 180, 0, NOW() - INTERVAL 2 DAY),
(5, 5, 'start', 0, 5, NOW() - INTERVAL 2 DAY + INTERVAL 1 SECOND),
(6, 3, 'view', 1500, 0, NOW() - INTERVAL 3 DAY),
(6, 3, 'start', 0, 25, NOW() - INTERVAL 3 DAY + INTERVAL 1 SECOND),
(6, 3, 'complete', 0, 100, NOW() - INTERVAL 3 DAY + INTERVAL 20 MINUTE);

-- ================================
-- 14. 资源收藏数据
-- ================================

INSERT INTO `resource_favorites` (`user_id`, `resource_id`) VALUES
(4, 1), (4, 2), (4, 6),
(5, 5), (5, 6),
(6, 1), (6, 3), (6, 8),
(7, 1), (7, 8);

-- ================================
-- 15. 学习计划数据
-- ================================

INSERT INTO `learning_plans` (`user_id`, `resource_id`, `status`, `progress`, `start_time`, `end_time`) VALUES
(4, 1, 'in_progress', 60, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 3 DAY),
(4, 2, 'pending', 0, NOW(), NOW() + INTERVAL 5 DAY),
(5, 5, 'in_progress', 30, NOW() - INTERVAL 2 DAY, NOW() + INTERVAL 2 DAY),
(6, 3, 'completed', 100, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 20 MINUTE),
(7, 8, 'pending', 0, NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 1 WEEK);

-- ================================
-- 16. 消息数据
-- ================================

INSERT INTO `messages` (`sender_id`, `receiver_id`, `title`, `content`, `message_type`, `status`, `send_time`, `read_time`) VALUES
(2, 4, '新资源提醒', '您关注的Java面向对象资源已更新，请查看最新内容。', 'notification', 'read', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 1 HOUR),
(2, 5, '任务提醒', '您的Java类与对象学习任务将在2天后到期，请按时完成。', 'notification', 'unread', NOW() - INTERVAL 1 DAY, NULL),
(1, 4, '系统维护通知', '系统将于今晚12点进行维护，预计耗时2小时。', 'system', 'read', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY),
(4, 5, '学习交流', '同学，最近在学Java的哪部分？可以一起讨论一下多态。', 'chat', 'unread', NOW() - INTERVAL 1 WEEK, NULL);

-- ================================
-- 17. 系统设置数据
-- ================================

INSERT INTO `system_settings` (`setting_key`, `setting_value`, `setting_desc`) VALUES
('site_name', 'EduAgent 智能学习平台', '网站名称'),
('site_description', '基于AI的个性化Java学习辅导平台，提供智能辅导和学习资源', '网站描述'),
('max_upload_size', '100', '最大上传文件大小(MB)'),
('session_timeout', '1800', '会话超时时间(秒)'),
('maintenance_mode', 'false', '维护模式开关'),
('registration_enabled', 'true', '是否开放注册');

-- ================================
-- 18. 项目数据
-- ================================

INSERT INTO `projects` (`user_id`, `title`, `description`, `course_id`, `difficulty`, `status`, `progress`) VALUES
(4, '学生成绩管理系统', '用Java Swing + 面向对象思想实现学生成绩的增删改查和统计分析', 'java_oop', '进阶', 'in_progress', 45),
(5, '简易计算器', '用Java基础语法实现一个控制台简易计算器，支持加减乘除', 'java_basics', '入门', 'draft', 0),
(7, '图书管理系统', '用集合框架实现图书入库、借阅、归还、查询功能', 'java_collections', '基础', 'completed', 100);

-- ================================
-- 显示插入结果
-- ================================

SELECT 'Mock data inserted successfully!' AS status;
SELECT COUNT(*) AS user_count FROM users;
SELECT COUNT(*) AS resource_count FROM resources;
SELECT COUNT(*) AS question_count FROM practice_questions;
SELECT COUNT(*) AS task_count FROM learning_tasks;
