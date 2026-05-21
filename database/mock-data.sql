-- EduAgent 演示数据
-- 作者: Ocean
-- 创建时间: 2026-05-20
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
(4, '计算机相关专业', '大二', 'Java 数据结构', '递归', '期末考 85 分', '有一定编程基础', '偏好图解、代码案例和练习题', '中速', '["递归", "二叉树"]', '["概念混淆", "边界条件遗漏"]', '["讲解文档", "思维导图", "练习题", "代码案例"]', '稳定提升型', '["建议围绕薄弱点进行查漏补缺。", "建议增加变式练习，提升知识迁移能力。", "建议定期复盘错题，形成稳定的解题方法。"]', 75),
(5, '信息类相关专业', '未知年级', '数据结构', '链表', '掌握并能应用当前知识点', '基础未知', '偏好结构化讲解', '中速', '["链表"]', '["概念混淆", "边界条件遗漏"]', '["讲解文档", "思维导图", "练习题", "代码案例"]', '基础补齐型', '["建议先补充前置知识，降低学习难度。", "建议采用分步骤讲解，每次只聚焦一个知识点。", "建议多使用图解、代码示例和基础练习。"]', 45),
(6, '计算机相关专业', '未知年级', 'Java 数据结构', '数据结构基础', '掌握并能应用当前知识点', '基础未知', '偏好结构化讲解', '中速', '["数据结构基础"]', '["概念混淆", "边界条件遗漏"]', '["讲解文档", "思维导图", "练习题", "代码案例"]', '基础补齐型', '["建议先补充前置知识，降低学习难度。", "建议采用分步骤讲解，每次只聚焦一个知识点。", "建议多使用图解、代码示例和基础练习。"]', 50),
(7, '信息类相关专业', '未知年级', '数据结构', '排序', '掌握并能应用当前知识点', '基础未知', '偏好结构化讲解', '中速', '["排序"]', '["概念混淆", "边界条件遗漏"]', '["讲解文档", "思维导图", "练习题", "代码案例"]', '稳定提升型', '["建议围绕薄弱点进行查漏补缺。", "建议增加变式练习，提升知识迁移能力。", "建议定期复盘错题，形成稳定的解题方法。"]', 70);

-- ================================
-- 4. 课程数据
-- ================================

INSERT INTO `courses` (`id`, `course_name`, `course_code`, `description`, `teacher_id`, `duration`, `difficulty`, `tags`, `status`) VALUES
('java_data_structure', 'Java 数据结构', 'CS201', '学习Java数据结构的核心概念和应用', 2, '16周', '基础', '["数据结构", "Java", "算法"]', 'published'),
('java_algorithms', 'Java 算法设计', 'CS202', '掌握常见算法的设计和实现', 2, '12周', '进阶', '["算法", "Java", "设计"]', 'published'),
('python_programming', 'Python 编程基础', 'CS101', 'Python编程语言基础入门', 3, '8周', '入门', '["Python", "编程", "基础"]', 'published');

-- ================================
-- 5. 学习资源数据
-- ================================

INSERT INTO `resources` (`title`, `type`, `difficulty`, `description`, `content`, `file_url`, `file_size`, `cover`, `author`, `rating`, `views`, `duration`, `course_id`, `course_name`, `chapter_name`, `tags`, `goals`, `suitable_for`, `status`, `teacher_id`) VALUES
('递归算法详解', '文档', '基础', '详细讲解递归算法的原理和应用', '# 递归算法详解\n\n## 什么是递归\n\n递归是一种解决问题的方法，它通过将问题分解为更小的子问题来求解。\n\n## 递归的两个要素\n\n1. **基准情况**：递归的终止条件\n2. **递归调用**：将问题分解为更小的子问题\n\n## 经典案例\n\n### 阶乘计算\n```java\npublic static int factorial(int n) {\n    if (n <= 1) {\n        return 1;\n    }\n    return n * factorial(n - 1);\n}\n```', '/uploads/resources/recursion_basic.pdf', '2.3MB', '/uploads/covers/recursion.jpg', '张教授', 4.5, 1200, '45分钟', 'java_data_structure', 'Java 数据结构', '递归与递归算法', '["递归", "算法", "基础"]', '["理解递归的基本概念", "掌握递归的终止条件", "能够写出简单的递归函数"]', '["初学者", "需要复习的学员"]', 'published', 2),

('二叉树遍历', '视频', '进阶', '讲解二叉树的各种遍历方式', NULL, '/uploads/resources/binary_tree_traversal.mp4', '156MB', '/uploads/covers/binary_tree.jpg', '张教授', 4.8, 3500, '60分钟', 'java_data_structure', 'Java 数据结构', '树与二叉树', '["二叉树", "遍历", "算法"]', '["掌握二叉树的前中后序遍历", "理解遍历的递归实现", "能够应用到实际项目中"]', '["有一定基础的学员", "算法学习者"]', 'published', 2),

('数据结构PPT', 'PPT', '基础', '数据结构课程完整PPT课件', NULL, '/uploads/resources/data_structure.pptx', '25MB', '/uploads/covers/data_structure.jpg', '张教授', 4.2, 800, '未知', 'java_data_structure', 'Java 数据结构', '课程总览', '["数据结构", "课件", "PPT"]', '["了解课程整体结构", "掌握重点知识点", "方便复习和预习"]', '["所有学员"]', 'published', 2),

('链表动画演示', '动画', '入门', '直观展示链表的操作过程', NULL, '/uploads/resources/linked_list_animation.swf', '5.6MB', '/uploads/covers/linked_list.gif', '李老师', 4.6, 2800, '10分钟', 'java_data_structure', 'Java 数据结构', '链表', '["链表", "动画", "可视化"]', '["直观理解链表结构", "掌握链表的基本操作", "理解指针的概念"]', '["初学者", "可视化学习者"]', 'published', 3),

('排序算法练习题', '题库', '进阶', '各种排序算法的练习题集合', NULL, '/uploads/resources/sorting_exercises.pdf', '3.1MB', '/uploads/covers/sorting.jpg', '张教授', 4.7, 1500, '未知', 'java_algorithms', 'Java 算法设计', '排序算法', '["排序", "算法", "练习"]', '["掌握各种排序算法的实现", "理解算法的时间复杂度", "能够选择合适的排序算法"]', '["有一定基础的学习者"]', 'published', 2),

('Java 链表实现', '代码案例', '基础', 'Java中链表的完整实现代码', '```java\n// 单链表节点\nclass ListNode {\n    int val;\n    ListNode next;\n\n    ListNode(int val) {\n        this.val = val;\n        this.next = null;\n    }\n}\n\n// 单链表类\nclass LinkedList {\n    ListNode head;\n\n    public void insert(int val) {\n        ListNode newNode = new ListNode(val);\n        if (head == null) {\n            head = newNode;\n        } else {\n            ListNode current = head;\n            while (current.next != null) {\n                current = current.next;\n            }\n            current.next = newNode;\n        }\n    }\n\n    public void printList() {\n        ListNode current = head;\n        while (current != null) {\n            System.out.print(current.val + \" -> \");\n            current = current.next;\n        }\n        System.out.println(\"null\");\n    }\n}\n```', NULL, '1.2KB', '/uploads/covers/java_code.jpg', '李老师', 4.4, 900, '未知', 'java_data_structure', 'Java 数据结构', '链表', '["链表", "Java", "代码"]', '["理解链表的Java实现", "能够自己实现链表的基本操作", "掌握指针操作"]', '["Java学习者", "实践型学习者"]', 'published', 3),

('数组操作实验', '实验项目', '中级', '数组各种操作的实验项目', NULL, '/uploads/resources/array_experiment.zip', '45KB', '/uploads/covers/array_exp.jpg', '李老师', 4.3, 600, '2小时', 'java_data_structure', 'Java 数据结构', '数组', '["数组", "实验", "实践"]', '["掌握数组的基本操作", "理解数组的内存分配", "能够解决数组相关问题"]', '["实践型学习者"]', 'published', 3),

('算法拓展阅读', '拓展阅读', '高级', '算法进阶阅读材料', NULL, '/uploads/resources/algorithm_reading.pdf', '8.9MB', '/uploads/covers/algorithm.jpg', '张教授', 4.5, 450, '未知', 'java_algorithms', 'Java 算法设计', '算法进阶', '["算法", "阅读", "拓展"]', '["拓展算法知识", "了解前沿算法", "提升算法思维"]', '["算法爱好者", "进阶学习者"]', 'published', 2);

-- ================================
-- 6. 资源章节数据
-- ================================

INSERT INTO `resource_chapters` (`resource_id`, `title`, `description`, `duration`, `content`, `order_num`) VALUES
(1, '递归的基本概念', '什么是递归，递归的基本特征', '5分钟', '# 递归的基本概念\n\n递归是一种通过函数调用自身来解决问题的方法。', 1),
(1, '递归的两个要素', '基准情况和递归调用', '10分钟', '# 递归的两个要素\n\n1. 基准情况：递归的终止条件\n2. 递归调用：将问题分解为更小的子问题', 2),
(1, '递归的经典案例', '阶乘、斐波那契数列等经典递归案例', '15分钟', '# 递归的经典案例\n\n## 阶乘\n```java\npublic static int factorial(int n) {\n    if (n <= 1) return 1;\n    return n * factorial(n - 1);\n}\n```', 3),
(2, '二叉树基本概念', '二叉树的定义和基本性质', '10分钟', '# 二叉树基本概念\n\n二叉树是每个节点最多有两个子树的树结构。', 1),
(2, '前序遍历', '前序遍历的原理和实现', '15分钟', '# 前序遍历\n\n前序遍历顺序：根节点 -> 左子树 -> 右子树', 2),
(2, '中序遍历', '中序遍历的原理和实现', '15分钟', '# 中序遍历\n\n中序遍历顺序：左子树 -> 根节点 -> 右子树', 3),
(2, '后序遍历', '后序遍历的原理和实现', '20分钟', '# 后序遍历\n\n后序遍历顺序：左子树 -> 右子树 -> 根节点', 4);

-- ================================
-- 7. 学习任务数据
-- ================================

INSERT INTO `learning_tasks` (`user_id`, `title`, `description`, `course_name`, `chapter_name`, `start_time`, `end_time`, `priority`, `status`, `progress`) VALUES
(4, '完成递归算法学习', '学习递归算法的基本概念和应用', 'Java 数据结构', '递归与递归算法', NOW() - INTERVAL 2 HOUR, NOW() + INTERVAL 4 HOUR, 'high', 'doing', 60),
(4, '学习二叉树遍历', '掌握二叉树的前中后序遍历', 'Java 数据结构', '树与二叉树', NOW(), NOW() + INTERVAL 1 DAY, 'high', 'todo', 0),
(5, '链表基础学习', '了解链表的基本概念和操作', 'Java 数据结构', '链表', NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 3 DAYS, 'middle', 'doing', 30),
(6, '排序算法练习', '完成排序算法相关练习题', 'Java 算法设计', '排序算法', NOW() - INTERVAL 3 DAYS, NOW() + INTERVAL 1 DAY, 'low', 'done', 100),
(7, '数据结构复习', '复习已学数据结构知识点', 'Java 数据结构', '课程总览', NOW() - INTERVAL 1 WEEK, NOW() + INTERVAL 1 WEEK, 'middle', 'todo', 0);

-- ================================
-- 8. 练习题数据
-- ================================

INSERT INTO `practice_questions` (`type`, `difficulty`, `topic`, `course_id`, `chapter`, `question`, `options`, `answer`, `analysis`, `tags`, `score`, `status`, `teacher_id`) VALUES
('选择题', '基础', '递归', 'java_data_structure', '递归与递归算法', '递归函数必须包含哪两个要素？', '["参数和返回值", "基准情况（终止条件）和递归调用", "变量和常量", "循环和条件"]', '基准情况（终止条件）和递归调用', '递归函数必须明确终止条件（防止无限递归）和递归调用（将问题分解为更小的子问题）。', '["递归", "算法", "基础"]', 10, 'published', 2),

('判断题', '基础', '递归', 'java_data_structure', '递归与递归算法', '递归函数一定能解决所有问题。', NULL, '错误', '递归不是万能的，有些问题用迭代解决更好，而且递归可能导致栈溢出。', '["递归", "算法"]', 5, 'published', 2),

('填空题', '基础', '递归', 'java_data_structure', '递归与递归算法', '递归的终止条件也叫______情况。', NULL, '基准', '递归的终止条件也叫基准情况，它是递归函数停止调用的条件。', '["递归", "概念"]', 5, 'published', 2),

('简答题', '中等', '递归', 'java_data_structure', '递归与递归算法', '请用递归实现计算n的阶乘。', NULL, '```java\npublic static int factorial(int n) {\n    if (n <= 1) return 1;\n    return n * factorial(n - 1);\n}\n```', '阶乘是递归的经典案例，n! = n × (n-1)!，基准情况是 0! = 1 或 1! = 1。', '["递归", "阶乘", "算法"]', 15, 'published', 2),

('代码题', '提高', '递归', 'java_data_structure', '递归与递归算法', '用递归实现斐波那契数列的第n项。', NULL, '```java\npublic static int fibonacci(int n) {\n    if (n <= 1) return n;\n    return fibonacci(n - 1) + fibonacci(n - 2);\n}\n```', '斐波那契数列：F(n) = F(n-1) + F(n-2)，基准情况是 F(0)=0, F(1)=1。', '["递归", "斐波那契", "算法"]', 20, 'published', 2),

('选择题', '中等', '二叉树', 'java_data_structure', '树与二叉树', '对于完全二叉树，如果节点索引从1开始，节点i的左子节点索引是？', '["2i", "2i + 1", "i/2", "i + 1"]', '2i', '完全二叉树使用数组存储时，节点i的左子节点索引为2i，右子节点索引为2i+1。', '["二叉树", "完全二叉树", "数组存储"]', 10, 'published', 2),

('选择题', '基础', '链表', 'java_data_structure', '链表', '链表相比数组的主要优势是？', '["随机访问效率高", "插入删除操作效率高", "内存占用小", "实现简单"]', '插入删除操作效率高', '链表在插入删除操作时只需要修改指针，时间复杂度为O(1)，而数组需要移动元素，时间复杂度为O(n)。', '["链表", "数据结构"]', 10, 'published', 3),

('判断题', '基础', '链表', 'java_data_structure', '链表', '链表需要连续的内存空间。', NULL, '错误', '链表不需要连续的内存空间，每个节点可以分散存储，通过指针连接。', '["链表", "内存"]', 5, 'published', 3);

-- ================================
-- 9. 练习记录数据
-- ================================

INSERT INTO `practice_records` (`user_id`, `question_id`, `user_answer`, `is_correct`, `score`, `time_spent`, `practice_time`) VALUES
(4, 1, '基准情况（终止条件）和递归调用', 1, 10, 15, NOW() - INTERVAL 1 HOUR),
(4, 2, '错误', 1, 5, 5, NOW() - INTERVAL 1 HOUR),
(4, 3, '基准', 1, 5, 8, NOW() - INTERVAL 1 HOUR),
(4, 4, '```java\npublic static int factorial(int n) {\n    if (n <= 1) return 1;\n    return n * factorial(n - 1);\n}\n```', 1, 15, 120, NOW() - INTERVAL 50 MINUTE),
(5, 7, '插入删除操作效率高', 1, 10, 20, NOW() - INTERVAL 30 MINUTE),
(5, 8, '错误', 1, 5, 10, NOW() - INTERVAL 25 MINUTE),
(6, 6, '2i', 1, 10, 25, NOW() - INTERVAL 3 DAYS);

-- ================================
-- 10. 辅导会话数据
-- ================================

INSERT INTO `tutor_sessions` (`user_id`, `session_id`, `title`, `topic`, `status`) VALUES
(4, 'session_001', '递归学习辅导', '递归', 'active'),
(5, 'session_002', '链表基础问答', '链表', 'active'),
(6, 'session_003', '排序算法讨论', '排序', 'ended');

-- ================================
-- 11. 辅导消息数据
-- ================================

INSERT INTO `tutor_messages` (`session_id`, `user_id`, `role`, `content`, `message_time`) VALUES
('session_001', 4, 'user', '如何理解递归的终止条件？', NOW() - INTERVAL 1 HOUR),
('session_001', 4, 'assistant', '递归的终止条件也叫基准情况，它是递归函数停止调用的条件。没有终止条件，递归会无限调用，导致栈溢出。', NOW() - INTERVAL 1 HOUR + INTERVAL 5 SECOND),
('session_001', 4, 'user', '那递归调用是什么意思？', NOW() - INTERVAL 1 HOUR + INTERVAL 2 MINUTE),
('session_001', 4, 'assistant', '递归调用是指在函数内部调用自身，将原问题分解为更小的子问题来解决。每次递归调用都会让问题规模变小，最终达到终止条件。', NOW() - INTERVAL 1 HOUR + INTERVAL 2 MINUTE + INTERVAL 5 SECOND),
('session_002', 5, 'user', '链表和数组有什么区别？', NOW() - INTERVAL 30 MINUTE),
('session_002', 5, 'assistant', '链表和数组的主要区别：\n1. 内存：数组需要连续内存，链表不需要\n2. 访问：数组支持随机访问，链表只能顺序访问\n3. 操作：链表插入删除快，数组查找快\n4. 大小：数组大小固定，链表可以动态扩展', NOW() - INTERVAL 30 MINUTE + INTERVAL 10 SECOND);

-- ================================
-- 12. 辅导建议数据
-- ================================

INSERT INTO `tutor_suggestions` (`session_id`, `title`, `prompt`, `order_num`) VALUES
('session_001', '递归基础', '请详细讲解递归的基础概念', 1),
('session_001', '递归练习', '给我一些递归练习题', 2),
('session_001', '递归应用', '递归在实际开发中的应用场景', 3),
('session_002', '链表实现', 'Java中如何实现链表', 1),
('session_002', '链表操作', '链表的插入和删除操作', 2);

-- ================================
-- 13. 学习记录数据
-- ================================

INSERT INTO `learning_records` (`user_id`, `resource_id`, `action`, `duration`, `progress`, `record_time`) VALUES
(4, 1, 'view', 300, 0, NOW() - INTERVAL 1 DAY),
(4, 1, 'start', 0, 10, NOW() - INTERVAL 1 DAY + INTERVAL 1 SECOND),
(4, 1, 'progress', 600, 60, NOW() - INTERVAL 23 HOUR),
(4, 2, 'view', 900, 0, NOW() - INTERVAL 12 HOUR),
(4, 2, 'start', 0, 20, NOW() - INTERVAL 12 HOUR + INTERVAL 1 SECOND),
(5, 5, 'view', 150, 0, NOW() - INTERVAL 2 DAY),
(5, 5, 'start', 0, 5, NOW() - INTERVAL 2 DAY + INTERVAL 1 SECOND),
(6, 3, 'view', 1200, 0, NOW() - INTERVAL 3 DAY),
(6, 3, 'start', 0, 25, NOW() - INTERVAL 3 DAY + INTERVAL 1 SECOND),
(6, 3, 'complete', 0, 100, NOW() - INTERVAL 3 DAY + INTERVAL 20 MINUTE);

-- ================================
-- 14. 资源收藏数据
-- ================================

INSERT INTO `resource_favorites` (`user_id`, `resource_id`) VALUES
(4, 1), (4, 2), (4, 6),
(5, 5), (5, 6),
(6, 1), (6, 3), (6, 7),
(7, 1), (7, 8);

-- ================================
-- 15. 学习计划数据
-- ================================

INSERT INTO `learning_plans` (`user_id`, `resource_id`, `status`, `progress`, `start_time`, `end_time`) VALUES
(4, 1, 'in_progress', 60, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 3 DAYS),
(4, 2, 'pending', 0, NOW(), NOW() + INTERVAL 5 DAYS),
(5, 5, 'in_progress', 30, NOW() - INTERVAL 2 DAY, NOW() + INTERVAL 2 DAYS),
(6, 3, 'completed', 100, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 20 MINUTE),
(7, 8, 'pending', 0, NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 1 WEEK);

-- ================================
-- 16. 消息数据
-- ================================

INSERT INTO `messages` (`sender_id`, `receiver_id`, `title`, `content`, `message_type`, `status`, `send_time`, `read_time`) VALUES
(2, 4, '新资源提醒', '您关注的递归算法资源已更新，请查看最新内容。', 'notification', 'read', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 1 HOUR),
(2, 5, '任务提醒', '您的链表学习任务将在2天后到期，请按时完成。', 'notification', 'unread', NOW() - INTERVAL 1 DAY, NULL),
(1, 4, '系统维护通知', '系统将于今晚12点进行维护，预计耗时2小时。', 'system', 'read', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 DAY),
(4, 5, '学习交流', '同学，最近在学什么？可以一起讨论一下。', 'chat', 'unread', NOW() - INTERVAL 1 WEEK, NULL);

-- ================================
-- 17. 系统设置数据
-- ================================

INSERT INTO `system_settings` (`setting_key`, `setting_value`, `setting_desc`) VALUES
('site_name', 'EduAgent 智能学习平台', '网站名称'),
('site_description', '基于AI的个性化学习平台，提供智能辅导和学习资源', '网站描述'),
('max_upload_size', '100', '最大上传文件大小(MB)'),
('session_timeout', '1800', '会话超时时间(秒)'),
('maintenance_mode', 'false', '维护模式开关'),
('registration_enabled', 'true', '是否开放注册');

-- ================================
-- 18. 项目数据
-- ================================

INSERT INTO `projects` (`user_id`, `title`, `description`, `course_id`, `difficulty`, `status`, `progress`) VALUES
(4, '实现二叉树搜索功能', '实现一个完整的二叉搜索树，包括插入、删除、查找功能', 'java_data_structure', '进阶', 'in_progress', 45),
(5, '链表数据结构实现', '从零实现单向链表和双向链表', 'java_data_structure', '基础', 'draft', 0),
(6, '排序算法可视化', '制作排序算法的可视化演示', 'java_algorithms', '进阶', 'completed', 100);

-- ================================
-- 显示插入结果
-- ================================

SELECT 'Mock data inserted successfully!' AS status;
SELECT COUNT(*) AS user_count FROM users;
SELECT COUNT(*) AS resource_count FROM resources;
SELECT COUNT(*) AS question_count FROM practice_questions;
SELECT COUNT(*) AS task_count FROM learning_tasks;