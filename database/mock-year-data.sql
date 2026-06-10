-- ============================================================
-- 一年帐户模拟数据 (v4.1.0)
-- 学生: 张三 (student_id = 100)
-- 课程: JavaSE 学习一年
-- 注册: 2025-06-10 ~ 至今 (365天)
-- ============================================================

-- ==================== 1. 用户 ====================
INSERT INTO `users` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `avatar`, `role`, `status`, `onboarded`, `last_login_time`, `last_login_ip`, `create_time`, `update_time`)
VALUES (100, 'zhangsan', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQm4sEPhMNPfFhpYN76u',
        '张三', 'zhangsan@edu.com', '13800138000', NULL,
        'student', 'active', 1, '2026-06-10 08:30:00', '192.168.1.100',
        '2025-06-10 09:00:00', '2026-06-10 08:30:00');

-- ==================== 2. 学习画像 ====================
INSERT INTO `student_profiles` (`id`, `student_id`, `course`, `topic`, `learning_goal`, `knowledge_base`, `weaknesses`, `mistake_patterns`, `resource_preference`, `cognitive_style`, `pace`, `last_score`, `profile_data`, `profile_suggestions`, `last_suggestion`, `overall_type`, `create_time`, `update_time`)
VALUES (100, 100, 'JavaSE', 'Java核心基础', '扎实掌握Java SE全部核心知识，能独立开发控制台应用和简易桌面程序',
        '熟练掌握Java SE基础语法、面向对象、集合、IO',
        '["泛型深入","反射机制","网络编程","正则表达式"]',
        '["泛型边界理解不透","反射性能开销考虑不足","IO流选择混淆","异常处理不严谨"]',
        '["video","code","mindmap"]',
        '视觉型', '适中', 78,
        '{"knowledge_mastery":{"score":78,"level":"level_4","evidence":"完成150+练习题，正确率76%"},"learning_goal_clarity":{"score":85,"level":"level_4","evidence":"明确目标为扎实掌握JavaSE"},"cognitive_adaptation":{"score":72,"level":"level_3","evidence":"偏好视觉型学习+代码实践"},"mistake_avoidance":{"score":65,"level":"level_3","evidence":"错题重复率下降35%"},"learning_autonomy":{"score":90,"level":"level_5","evidence":"持续每日学习345天"},"overall_level":{"score":78,"level":"level_4","evidence":"可独立完成JavaSE小项目"}}',
        '1. 重点巩固泛型和反射机制，建议手写泛型工具类实战\n2. 系统学习网络编程，完成一个简易HTTP服务器\n3. 多做编程题，培养代码规范意识\n4. 深入学习集合源码，理解数据结构原理',
        '建议本周重点：深入理解HashMap源码和泛型擦除机制',
        '稳定提升型', '2025-06-10 10:00:00', '2026-06-10 08:00:00');

-- ==================== 3. 学习路径 ====================
INSERT INTO `learning_paths` (`id`, `student_id`, `steps`, `progress`, `pace`, `goal`, `suggestions`, `recommendations`, `exam_advice`, `status`, `create_time`, `update_time`)
VALUES (100, 100,
        '[{"stage":1,"name":"Java SE 基础语法","duration":"2个月","tasks":["数据类型与运算符","流程控制","数组","面向对象基础"],"completed":true},{"stage":2,"name":"面向对象进阶","duration":"2个月","tasks":["继承与多态","抽象类与接口","异常处理","泛型"],"completed":true},{"stage":3,"name":"核心API","duration":"2.5个月","tasks":["集合框架","IO与NIO","多线程","网络编程"],"completed":true},{"stage":4,"name":"高级特性","duration":"2个月","tasks":["反射与注解","JVM基础","Lambda与Stream","正则表达式"],"completed":true},{"stage":5,"name":"综合实战","duration":"2.5个月","tasks":["迷你电商控制台","简易HTTP服务器","图书管理系统","数据结构练习"],"progress":65}]',
        72, '适中', 'JavaSE核心掌握者',
        '基础已扎实，建议转向JavaEE/框架学习',
        '推荐学习《Java核心技术卷》《Effective Java》',
        '建议重点复习：集合源码、IO模型、多线程基础',
        'active', '2025-06-12 09:00:00', '2026-06-09 22:00:00');

-- ==================== 4. 学习日志 (365天学习记录) ====================
-- 每周5天学习, 每天1-3个模块, 每个模块15-90分钟
DELIMITER //
CREATE PROCEDURE generate_study_logs()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE days_ago INT;
    DECLARE log_date DATETIME;
    DECLARE module_name VARCHAR(20);
    DECLARE num_modules INT;
    DECLARE j INT;
    DECLARE duration INT;
    DELETE FROM study_logs WHERE student_id = 100;
    WHILE i < 365 DO
        SET days_ago = 365 - i;
        SET log_date = DATE_SUB('2026-06-10 09:00:00', INTERVAL days_ago DAY);
        IF DAYOFWEEK(log_date) = 1 THEN SET num_modules = 0;
        ELSEIF DAYOFWEEK(log_date) = 7 THEN SET num_modules = FLOOR(RAND() * 3);
        ELSE SET num_modules = 1 + FLOOR(RAND() * 3);
        END IF;
        SET j = 0;
        WHILE j < num_modules DO
            SET module_name = ELT(1 + FLOOR(RAND() * 4), 'mindmap', 'quiz', 'reading', 'code');
            SET duration = 900 + FLOOR(RAND() * 4500);
            INSERT INTO `study_logs` (`student_id`, `module`, `duration_sec`, `chapter_id`, `note_id`, `created_at`)
            VALUES (100, module_name, duration,
                    NULLIF(1 + FLOOR(RAND() * 12), 0), NULLIF(1 + FLOOR(RAND() * 50), 0),
                    DATE_ADD(log_date, INTERVAL (8 + FLOOR(RAND() * 12)) HOUR));
            SET j = j + 1;
        END WHILE;
        SET i = i + 1;
    END WHILE;
END//
DELIMITER ;
CALL generate_study_logs();
DROP PROCEDURE IF EXISTS generate_study_logs;

-- ==================== 5. 练习题回答 (约200次练习) ====================
INSERT INTO `quiz_answer` (`student_id`, `resource_id`, `question`, `question_type`, `user_answer`, `correct_answer`, `is_correct`, `explanation`, `create_time`)
SELECT 100, 1 + FLOOR(RAND() * 50) AS resource_id,
       ELT(1 + FLOOR(RAND() * 8),
           'Java中HashMap的底层实现原理？',
           'equals()和hashCode()为什么要同时重写？',
           'final关键字有哪些用法？',
           'ArrayList和LinkedList的区别？',
           'String、StringBuilder、StringBuffer的区别？',
           'Java异常体系的结构是怎样的？',
           '接口和抽象类的区别？',
           'static关键字的作用？'
       ) AS question,
       'choice', ELT(1 + FLOOR(RAND() * 4), 'A','B','C','D'),
       ELT(1 + FLOOR(RAND() * 4), 'A','B','C','D'),
       CASE WHEN RAND() > 0.35 THEN 1 ELSE 0 END,
       ELT(1 + FLOOR(RAND() * 4),
           '参考《Java核心技术》第6章', '注意自反性、对称性、传递性',
           'final修饰变量/方法/类的区别', '底层数据结构不同决定性能差异',
           '不可变与可变字符串的区别', '受检与非受检异常的区别'
       ), DATE_ADD('2025-06-11 10:00:00', INTERVAL FLOOR(RAND() * 365) DAY)
FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) a
CROSS JOIN (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) b
CROSS JOIN (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) c
LIMIT 200;

-- ==================== 6. 对话记录 (约50次辅导对话) ====================
INSERT INTO `conversation` (`student_id`, `session_id`, `question`, `answer`, `intent`, `intent_confidence`, `evaluation_report`, `resource_dir`, `create_time`)
SELECT 100,
       CONCAT('session_', FLOOR(100000 + RAND() * 900000)),
       ELT(1 + FLOOR(RAND() * 8),
           '请解释Java内存模型', 'HashMap线程安全吗？', '泛型擦除是什么意思？',
           'Java8的Stream怎么用？', '什么是反射？', '多线程怎么保证原子性？',
           'try-with-resources的原理？', 'Comparable和Comparator的区别？'
       ),
       CONCAT('关于"', ELT(1 + FLOOR(RAND() * 8),
           'Java内存模型', 'HashMap', '泛型', 'Stream',
           '反射机制', '多线程', 'try-with-resources', 'Comparable/Comparator'
       ), '"的详细解答...（AI辅导内容）'),
       'tutor', 0.85 + RAND() * 0.15, NULL, NULL,
       DATE_ADD('2025-06-12 14:00:00', INTERVAL FLOOR(RAND() * 360) DAY)
FROM (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) a
CROSS JOIN (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) b
LIMIT 50;

-- ==================== 7. 学习任务 (JavaSE路线) ====================
INSERT INTO `learning_tasks` (`user_id`, `title`, `description`, `course_name`, `chapter_name`, `stage`, `start_time`, `end_time`, `priority`, `status`, `progress`, `create_time`, `update_time`)
VALUES
(100, 'Java基础语法练习', '完成数据类型、运算符、流程控制练习', 'JavaSE', '基础语法', '1',
 '2025-06-15 09:00:00', '2025-07-05 18:00:00', 'high', 'completed', 100, '2025-06-12 10:00:00', '2025-07-05 20:00:00'),
(100, '面向对象编程练习', '掌握封装、继承、多态，完成学生管理系统', 'JavaSE', '面向对象', '1',
 '2025-07-10 09:00:00', '2025-08-01 18:00:00', 'high', 'completed', 100, '2025-07-08 10:00:00', '2025-08-01 21:00:00'),
(100, '数组与排序算法', '学习数组操作、常用排序算法、二分查找', 'JavaSE', '数组与算法', '1',
 '2025-08-05 09:00:00', '2025-08-20 18:00:00', 'medium', 'completed', 100, '2025-08-02 10:00:00', '2025-08-20 19:00:00'),
(100, '集合框架深入学习', '掌握ArrayList/LinkedList/HashMap/TreeMap源码', 'JavaSE', '集合框架', '2',
 '2025-09-01 09:00:00', '2025-09-25 18:00:00', 'high', 'completed', 100, '2025-08-28 10:00:00', '2025-09-25 22:00:00'),
(100, '异常处理与泛型', '学习异常体系、自定义异常、泛型擦除机制', 'JavaSE', '异常与泛型', '2',
 '2025-10-01 09:00:00', '2025-10-15 18:00:00', 'high', 'completed', 100, '2025-09-28 10:00:00', '2025-10-15 20:00:00'),
(100, 'IO流与文件操作', '掌握字节流/字符流/NIO、文件读写实战', 'JavaSE', 'IO流', '2',
 '2025-10-20 09:00:00', '2025-11-05 18:00:00', 'medium', 'completed', 100, '2025-10-17 10:00:00', '2025-11-05 19:00:00'),
(100, '多线程基础', '学习Thread/Runnable、线程同步、线程池', 'JavaSE', '多线程', '3',
 '2025-11-10 09:00:00', '2025-12-05 18:00:00', 'high', 'completed', 100, '2025-11-07 10:00:00', '2025-12-05 21:00:00'),
(100, '网络编程入门', '学习Socket/TCP/UDP、完成简易聊天室', 'JavaSE', '网络编程', '3',
 '2025-12-10 09:00:00', '2025-12-30 18:00:00', 'high', 'completed', 100, '2025-12-07 10:00:00', '2025-12-30 20:00:00'),
(100, '反射与注解', '掌握反射API、自定义注解、动态代理', 'JavaSE', '反射机制', '4',
 '2026-01-05 09:00:00', '2026-01-25 18:00:00', 'medium', 'completed', 100, '2026-01-02 10:00:00', '2026-01-25 18:30:00'),
(100, 'Lambda与Stream API', '学习函数式编程、Stream流式操作、Optional', 'JavaSE', 'Lambda', '4',
 '2026-02-01 09:00:00', '2026-02-20 18:00:00', 'high', 'completed', 100, '2026-01-28 10:00:00', '2026-02-20 22:00:00'),
(100, 'JVM内存模型入门', '学习JVM内存分区、GC机制、类加载过程', 'JavaSE', 'JVM', '4',
 '2026-03-01 09:00:00', '2026-03-20 18:00:00', 'high', 'completed', 80, '2026-02-25 10:00:00', '2026-03-18 20:00:00'),
(100, '正则表达式与字符串处理', '掌握正则语法、Pattern/Matcher使用', 'JavaSE', '正则', '4',
 '2026-04-01 09:00:00', '2026-04-15 18:00:00', 'medium', 'completed', 100, '2026-03-28 10:00:00', '2026-04-15 19:00:00'),
(100, '图书管理系统综合实战', '运用JavaSE全部知识完成控制台图书管理系统', 'JavaSE', '综合实战', '5',
 '2026-05-01 09:00:00', '2026-06-15 18:00:00', 'high', 'doing', 55, '2026-04-28 10:00:00', '2026-06-09 22:00:00'),
(100, '数据结构算法练习', '刷LeetCode简单-中等题，巩固数据结构', 'JavaSE', '算法', '5',
 '2026-06-10 09:00:00', '2026-07-10 18:00:00', 'medium', 'todo', 0, '2026-06-08 10:00:00', '2026-06-08 10:00:00');

-- ==================== 8. 月度报告 (12个月) ====================
INSERT INTO `report` (`student_id`, `title`, `content`, `period_start`, `period_end`, `metrics`, `create_time`)
VALUES
(100, '2025年6月学习报告',
 '{"summary":"本月为入学首月，重点学习了Java基础语法和开发环境搭建","totalHours":32,"modules":{"mindmap":8,"quiz":10,"reading":7,"code":7},"score":55,"trend":"up"}',
 '2025-06-10','2025-06-30','{"avgDailyMinutes":64,"totalTasks":2,"completedTasks":1,"weaknessCount":6}','2025-06-30 22:00:00'),
(100, '2025年7月学习报告',
 '{"summary":"面向对象编程学习完成，掌握了封装继承多态三大特性","totalHours":48,"modules":{"mindmap":10,"quiz":15,"reading":8,"code":15},"score":60,"trend":"up"}',
 '2025-07-01','2025-07-31','{"avgDailyMinutes":93,"totalTasks":3,"completedTasks":2,"weaknessCount":5}','2025-07-31 21:00:00'),
(100, '2025年8月学习报告',
 '{"summary":"数组、排序算法和常用工具类学习，编程能力提升","totalHours":42,"modules":{"mindmap":8,"quiz":14,"reading":12,"code":8},"score":63,"trend":"up"}',
 '2025-08-01','2025-08-31','{"avgDailyMinutes":81,"totalTasks":2,"completedTasks":1,"weaknessCount":5}','2025-08-31 20:00:00'),
(100, '2025年9月学习报告',
 '{"summary":"集合框架深入学习，掌握了常用数据结构的底层原理","totalHours":52,"modules":{"mindmap":10,"quiz":18,"reading":12,"code":12},"score":67,"trend":"up"}',
 '2025-09-01','2025-09-30','{"avgDailyMinutes":104,"totalTasks":3,"completedTasks":3,"weaknessCount":4}','2025-09-30 21:00:00'),
(100, '2025年10月学习报告',
 '{"summary":"异常处理和泛型机制学习，代码健壮性提升","totalHours":45,"modules":{"mindmap":12,"quiz":10,"reading":8,"code":15},"score":70,"trend":"up"}',
 '2025-10-01','2025-10-31','{"avgDailyMinutes":87,"totalTasks":2,"completedTasks":1,"weaknessCount":4}','2025-10-31 20:00:00'),
(100, '2025年11月学习报告',
 '{"summary":"IO流与文件操作学习完成，掌握了NIO基础","totalHours":50,"modules":{"mindmap":8,"quiz":12,"reading":10,"code":20},"score":73,"trend":"up"}',
 '2025-11-01','2025-11-30','{"avgDailyMinutes":100,"totalTasks":3,"completedTasks":2,"weaknessCount":3}','2025-11-30 22:00:00'),
(100, '2025年12月学习报告',
 '{"summary":"多线程基础学习完成，理解了线程安全和锁机制","totalHours":46,"modules":{"mindmap":6,"quiz":15,"reading":10,"code":15},"score":75,"trend":"stable"}',
 '2025-12-01','2025-12-31','{"avgDailyMinutes":89,"totalTasks":2,"completedTasks":2,"weaknessCount":3}','2025-12-31 20:00:00'),
(100, '2026年1月学习报告',
 '{"summary":"网络编程入门，完成简易聊天室项目","totalHours":38,"modules":{"mindmap":5,"quiz":10,"reading":8,"code":15},"score":76,"trend":"stable"}',
 '2026-01-01','2026-01-31','{"avgDailyMinutes":74,"totalTasks":2,"completedTasks":1,"weaknessCount":3}','2026-01-31 21:00:00'),
(100, '2026年2月学习报告',
 '{"summary":"反射与注解学习完成，理解了动态代理机制","totalHours":44,"modules":{"mindmap":8,"quiz":14,"reading":8,"code":14},"score":78,"trend":"up"}',
 '2026-02-01','2026-02-28','{"avgDailyMinutes":94,"totalTasks":2,"completedTasks":2,"weaknessCount":2}','2026-02-28 20:00:00'),
(100, '2026年3月学习报告',
 '{"summary":"JVM入门学习，理解内存分区和GC机制","totalHours":40,"modules":{"mindmap":10,"quiz":12,"reading":10,"code":8},"score":78,"trend":"stable"}',
 '2026-03-01','2026-03-31','{"avgDailyMinutes":77,"totalTasks":2,"completedTasks":0,"weaknessCount":2}','2026-03-31 21:00:00'),
(100, '2026年4月学习报告',
 '{"summary":"正则表达式和字符串处理学习，文本处理能力提升","totalHours":42,"modules":{"mindmap":6,"quiz":8,"reading":10,"code":18},"score":80,"trend":"up"}',
 '2026-04-01','2026-04-30','{"avgDailyMinutes":84,"totalTasks":2,"completedTasks":1,"weaknessCount":2}','2026-04-30 20:00:00'),
(100, '2026年5月学习报告',
 '{"summary":"图书管理系统综合实战，综合运用JavaSE全部知识","totalHours":48,"modules":{"mindmap":4,"quiz":6,"reading":6,"code":32},"score":82,"trend":"up"}',
 '2026-05-01','2026-05-31','{"avgDailyMinutes":93,"totalTasks":2,"completedTasks":0,"weaknessCount":2}','2026-05-31 22:00:00');

-- ==================== 9. 学习路径历史 ====================
INSERT INTO `learning_path_history` (`student_id`, `goal`, `path_data`, `created_at`)
VALUES
(100, 'JavaSE基础入门', '{"stages":[{"name":"基础语法","duration":"1个月"},{"name":"面向对象","duration":"1个月"}]}', '2025-06-12 09:00:00'),
(100, 'JavaSE核心进阶', '{"stages":[{"name":"集合与IO","duration":"2个月"},{"name":"多线程","duration":"1.5个月"}]}', '2025-09-01 09:00:00'),
(100, 'JavaSE高级特性', '{"stages":[{"name":"反射与注解","duration":"1.5个月"},{"name":"Lambda与Stream","duration":"1个月"}]}', '2025-12-01 09:00:00'),
(100, 'JavaSE综合实战', '{"stages":[{"name":"综合项目","duration":"2.5个月"},{"name":"算法巩固","duration":"1个月"}]}', '2026-03-01 09:00:00');

-- ==================== 10. 资源记录 (AI生成的JavaSE资源) ====================
INSERT INTO `resources` (`title`, `type`, `difficulty`, `description`, `content`, `author`, `rating`, `views`, `favorites`, `student_id`, `course_name`, `chapter_name`, `tags`, `status`, `create_time`, `update_time`)
VALUES
('Java基础语法思维导图', 'mindmap', 'easy', 'Java基础语法全景：数据类型、运算符、流程控制', '```mindmap\n# Java基础语法\n## 数据类型\n- 基本类型(8种)\n- 引用类型\n## 流程控制\n- if/switch\n- for/while\n```', 'AI系统', 4.5, 200, 25, 100, 'JavaSE', '基础语法', '["Java","基础语法","入门"]', 'published', '2025-06-15 10:00:00', '2025-06-15 10:00:00'),
('面向对象编程练习题', 'quiz', 'medium', '封装、继承、多态综合练习', '[{"q":"Java支持多继承吗？","options":["A.支持","B.不支持","C.通过接口支持","D.A和C"],"answer":"D"}]', 'AI系统', 4.3, 150, 18, 100, 'JavaSE', '面向对象', '["Java","OOP","练习"]', 'published', '2025-07-12 14:00:00', '2025-07-12 14:00:00'),
('集合框架源码分析', 'reading', 'hard', 'ArrayList/LinkedList/HashMap 源码深度解析', '## 集合框架\n### ArrayList\n- 基于数组实现\n- 扩容倍数1.5倍\n- 随机访问O(1)\n### HashMap\n- 数组+链表+红黑树\n- 负载因子0.75\n- 扩容为2倍', 'AI系统', 4.8, 300, 35, 100, 'JavaSE', '集合框架', '["集合","源码","数据结构"]', 'published', '2025-09-05 16:00:00', '2025-09-05 16:00:00'),
('IO流代码案例', 'code', 'medium', '字节流/字符流/NIO文件读写完整示例', '// 字节流复制文件\ntry (FileInputStream fis = new FileInputStream(\"src.txt\");\n     FileOutputStream fos = new FileOutputStream(\"dst.txt\")) {\n    byte[] buffer = new byte[1024];\n    int len;\n    while ((len = fis.read(buffer)) != -1) {\n        fos.write(buffer, 0, len);\n    }\n}', 'AI系统', 4.6, 180, 22, 100, 'JavaSE', 'IO流', '["IO","NIO","文件操作"]', 'published', '2025-10-25 11:00:00', '2025-10-25 11:00:00'),
('多线程思维导图', 'mindmap', 'medium', '多线程核心知识全景：创建方式、同步、锁、线程池', '```mindmap\n# 多线程\n## 创建方式\n- Thread\n- Runnable\n- Callable\n## 同步\n- synchronized\n- Lock\n- volatile\n```', 'AI系统', 4.7, 250, 30, 100, 'JavaSE', '多线程', '["多线程","并发","锁"]', 'published', '2025-11-15 15:00:00', '2025-11-15 15:00:00'),
('网络编程代码案例', 'code', 'hard', 'TCP Socket编程、简易聊天室完整代码', '// 简易聊天室服务端\nServerSocket server = new ServerSocket(8888);\nSystem.out.println(\"服务器启动...\");\nwhile (true) {\n    Socket socket = server.accept();\n    new Thread(new Handler(socket)).start();\n}', 'AI系统', 4.5, 160, 20, 100, 'JavaSE', '网络编程', '["Socket","TCP","网络"]', 'published', '2025-12-15 09:00:00', '2025-12-15 09:00:00'),
('反射与注解练习题', 'quiz', 'hard', '反射API、自定义注解、动态代理综合练习', '[{"q":"获取Class对象的三种方式？","options":["A.Class.forName","B.类.class","C.对象.getClass","D.以上都是"],"answer":"D"}]', 'AI系统', 4.4, 120, 15, 100, 'JavaSE', '反射机制', '["反射","注解","动态代理"]', 'published', '2026-01-10 14:00:00', '2026-01-10 14:00:00'),
('Lambda与Stream代码案例', 'code', 'medium', '函数式编程、Stream流式操作、Optional使用', 'List<String> list = Arrays.asList(\"Java\", \"Python\", \"Go\");\nlist.stream()\n    .filter(s -> s.length() > 2)\n    .map(String::toUpperCase)\n    .forEach(System.out::println);', 'AI系统', 4.6, 140, 18, 100, 'JavaSE', 'Lambda', '["Lambda","Stream","函数式"]', 'published', '2026-02-05 10:00:00', '2026-02-05 10:00:00'),
('图书管理系统完整代码', 'code', 'hard', 'JavaSE图书管理系统：集合+IO+异常+面向对象综合', 'public class LibrarySystem {\n    private List<Book> books = new ArrayList<>();\n    private Map<String, Reader> readers = new HashMap<>();\n\n    public void addBook(Book book) {\n        books.add(book);\n        System.out.println(\"添加成功: \" + book);\n    }\n    // 借书、还书、查询...\n}', 'AI系统', 4.8, 220, 28, 100, 'JavaSE', '综合实战', '["项目实战","图书管理","JavaSE"]', 'published', '2026-05-05 09:00:00', '2026-05-05 09:00:00');
