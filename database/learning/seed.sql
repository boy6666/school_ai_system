-- =====================================================================
-- edu-agent-learning · learning_db 种子数据（教师端看板联调用，不进前端）
-- 前置：先执行 init.sql。用法：mysql -uroot -p learning_db < database/learning/seed.sql
-- 说明：student_id=1001/1002 为测试学生（需在 auth_db.users 存在同 id 账号，
--       密码统一 admin123，由 auth 侧 seed 提供）；class_id=1/2 逻辑引用 teacher_db.classes。
-- =====================================================================
USE learning_db;

-- ---------- 学生画像 ----------
INSERT INTO student_profiles
  (student_id, class_id, major, grade, course, topic, learning_goal, knowledge_base,
   cognitive_style, pace, weaknesses, mistake_patterns, resource_preference,
   overall_type, last_score, profile_data, profile_suggestions, profile_complete)
VALUES
 (1001, 1, '计算机科学与技术', '大二', 'JavaSE', '面向对象',
  '两个月内掌握 Java 面向对象与集合框架，能独立完成课程设计', '有 C 语言基础，Java 入门',
  'visual', 'medium',
  '["多态","集合框架","异常处理"]', '["概念混淆","边界条件遗漏"]', '["mindmap","quiz","reading"]',
  '稳定提升型', 72,
  '{"knowledge_mastery":{"score":70,"level":"level_2","evidence":["测验正确率约 70%"]},"learning_goal_clarity":{"score":65,"level":"level_2","evidence":["目标明确但拆解不足"]},"cognitive_adaptation":{"score":60,"level":"level_2","evidence":["偏好可视化讲解"]},"mistake_avoidance":{"score":55,"level":"level_2","evidence":["同类错题重复出现 2 次"]},"learning_autonomy":{"score":68,"level":"level_2","evidence":["日均学习 40 分钟"]},"overall_level":{"score":64,"level":"level_2","evidence":["六维加权综合"]}}',
  '优先巩固多态与集合框架\n每天完成 10 道选择题\n每周输出一篇学习笔记', 1),
 (1002, 1, '软件工程', '大二', 'JavaSE', '多线程',
  '期末前掌握多线程与 IO，完成课后实验', '零基础起步，已学完语法',
  'reading', 'slow',
  '["线程安全","IO流"]', '["API 记忆不牢"]', '["reading","code"]',
  '稳步扎实型', 58,
  '{"knowledge_mastery":{"score":52,"level":"level_1","evidence":["测验正确率约 52%"]},"learning_goal_clarity":{"score":70,"level":"level_2","evidence":["目标清晰可执行"]},"cognitive_adaptation":{"score":60,"level":"level_2","evidence":["偏好文档阅读"]},"mistake_avoidance":{"score":50,"level":"level_1","evidence":["IO 类名混淆频繁"]},"learning_autonomy":{"score":55,"level":"level_2","evidence":["学习间隔不稳定"]},"overall_level":{"score":57,"level":"level_1","evidence":["六维加权综合"]}}',
  '从线程生命周期概念重建知识框架\n结合小案例练 IO 流\n错题当日复盘', 1);

-- ---------- 学习路径 ----------
INSERT INTO learning_paths (student_id, steps, progress, pace, goal, suggestions, status) VALUES
 (1001,
  '{"goal":"掌握面向对象与集合框架","targetMastery":"≥85%","totalHours":24,"masteryRate":70,"totalTasks":6,"completedTasks":2,"stages":[{"name":"今日计划","tasks":[{"title":"复习多态的概念与实现方式","duration":30,"status":2,"progress":100},{"title":"做 10 道集合框架选择题","duration":45,"status":0,"progress":0}]},{"name":"本周路径","tasks":[{"title":"整理接口与抽象类对比笔记","duration":60,"status":2,"progress":100},{"title":"完成 ArrayList/LinkedList 对比实验","duration":60,"status":0,"progress":0}]}],"suggestions":"多态部分建议结合案例二次复习","applicationAdvice":"用集合框架改造之前的数组作业","examAdvice":"重点复习继承、多态与异常","recommendTime":"每天 19:00-21:00"}',
  33, 'medium', '掌握面向对象与集合框架', '多态部分建议结合案例二次复习', 'active'),
 (1002,
  '{"goal":"掌握多线程与 IO 基础","targetMastery":"≥80%","totalHours":18,"masteryRate":52,"totalTasks":4,"completedTasks":1,"stages":[{"name":"今日计划","tasks":[{"title":"阅读线程生命周期章节","duration":40,"status":2,"progress":100},{"title":"整理 IO 流类层次图","duration":30,"status":0,"progress":0}]},{"name":"考试冲刺","tasks":[{"title":"线程同步经典题练习","duration":60,"status":0,"progress":0},{"title":"IO 流代码默写练习","duration":45,"status":0,"progress":0}]}],"suggestions":"先补线程概念再做题","applicationAdvice":"写一个多线程下载小工具","examAdvice":"重点复习线程安全与 IO 体系","recommendTime":"每天 20:00-22:00"}',
  25, 'slow', '掌握多线程与 IO 基础', '先补线程概念再做题', 'active');

-- ---------- 学习任务 ----------
INSERT INTO learning_tasks (user_id, title, description, course_name, chapter_name, stage, priority, status, progress) VALUES
 (1001, '复习多态的概念与实现方式', '重读多态章节并总结三种实现', 'JavaSE', '面向对象', 'today', 'high', 'done', 100),
 (1001, '做 10 道集合框架选择题', '错题当日复盘', 'JavaSE', '集合框架', 'today', 'high', 'todo', 0),
 (1001, '整理接口与抽象类对比笔记', '输出对比表格', 'JavaSE', '面向对象', 'week', 'middle', 'done', 100),
 (1001, '完成 ArrayList/LinkedList 对比实验', '记录性能差异', 'JavaSE', '集合框架', 'week', 'middle', 'todo', 0),
 (1001, '异常处理专项测验', '10 题 + 错题讲解', 'JavaSE', '异常', 'exam', 'high', 'todo', 0),
 (1002, '阅读线程生命周期章节', '结合思维导图', 'JavaSE', '多线程', 'today', 'high', 'done', 100),
 (1002, '整理 IO 流类层次图', '字节流/字符流分叉', 'JavaSE', 'IO', 'today', 'middle', 'todo', 0),
 (1002, '线程同步经典题练习', '生产者消费者模型', 'JavaSE', '多线程', 'exam', 'high', 'todo', 0);

-- ---------- 学习路径历史 ----------
INSERT INTO learning_path_history (student_id, goal, path_data) VALUES
 (1001, '掌握面向对象与集合框架', '{"snapshot":"v1","totalTasks":6,"progress":0}'),
 (1001, '掌握面向对象与集合框架', '{"snapshot":"v2","totalTasks":6,"progress":33}'),
 (1002, '掌握多线程与 IO 基础', '{"snapshot":"v1","totalTasks":4,"progress":25}');

-- ---------- 学习日志（近 7 天） ----------
INSERT INTO study_logs (student_id, module, duration_sec, chapter_id, note_id, created_at) VALUES
 (1001, 'mindmap', 1200, 3, NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
 (1001, 'quiz',     900, 3, NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
 (1001, 'reading', 1800, 4, NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
 (1001, 'quiz',    1500, 4, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
 (1001, 'code',     600, 4, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
 (1001, 'mindmap',  900, 5, NULL, NOW()),
 (1002, 'reading', 2400, 8, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),
 (1002, 'quiz',     720, 8, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY)),
 (1002, 'reading', 1200, 9, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ---------- 测验作答（含错题） ----------
INSERT INTO quiz_answer (student_id, resource_id, question, question_type, user_answer, correct_answer, is_correct, explanation) VALUES
 (1001, 501, 'Java 中实现多态的途径不包括？', 'choice', '方法重写', '方法重载', 0, '重载是编译期绑定，不属于运行时多态。'),
 (1001, 501, 'ArrayList 底层使用的数据结构是？', 'choice', '动态数组', '动态数组', 1, '正确，ArrayList 基于动态数组实现。'),
 (1001, 502, '接口中能否定义成员变量？', 'choice', '可以，且为 public static final', '可以，且为 public static final', 1, '接口字段默认 public static final。'),
 (1001, 502, '下面哪个是受检异常？', 'choice', 'NullPointerException', 'IOException', 0, 'IOException 编译期强制处理，属受检异常。'),
 (1001, 503, '简述多态的概念与好处。', 'short', '同一个方法在不同子类有不同实现', NULL, NULL, 'AI 判分：要点基本覆盖，建议补充"消除类型耦合"的好处。'),
 (1002, 501, '启动线程应调用哪个方法？', 'choice', 'run()', 'start()', 0, 'start() 会创建新线程并回调 run()。'),
 (1002, 501, '字节流的抽象基类是？', 'choice', 'Reader', 'InputStream', 0, 'Reader 是字符流基类；字节流为 InputStream/OutputStream。'),
 (1002, 502, '线程进入终止态的条件？', 'choice', 'run() 执行结束', 'run() 执行结束', 1, '正确。');

-- ---------- 报告 ----------
INSERT INTO report (student_id, title, content, period_start, period_end, metrics, create_time) VALUES
 (1001, 'AI学习总结 - 2026-09-10',
  '{"summary":"本周面向对象模块稳步推进，多态概念已基本掌握。","strengths":"任务完成及时，笔记习惯好。","weaknessAnalysis":"异常处理章节错误率偏高。","suggestion":"下周集中补异常处理专项。","score":72,"focusNext":"异常处理"}',
  DATE_SUB(CURDATE(), INTERVAL 7 DAY), CURDATE(), NULL, NOW()),
 (1002, 'AI学习总结 - 2026-09-10',
  '{"summary":"多线程章节起步，概念题正确率偏低。","strengths":"阅读时长稳定。","weaknessAnalysis":"API 类名混淆，线程概念不牢。","suggestion":"先完成线程生命周期思维导图。","score":58,"focusNext":"线程生命周期"}',
  DATE_SUB(CURDATE(), INTERVAL 7 DAY), CURDATE(), NULL, NOW());

-- ---------- 对话历史 ----------
INSERT INTO conversation (student_id, session_id, question, answer, intent, resource_dir) VALUES
 (1001, 'tutor_1001_01', '什么是多态？能举个例子吗？',
  '多态指同一接口在不同实现下表现出不同行为。例如 Animal.sound() 在 Cat/Dog 子类中分别输出不同叫声……', 'explain', NULL),
 (1001, 'tutor_1001_02', 'ArrayList 和 LinkedList 该怎么选？',
  '随机访问多用 ArrayList，频繁头尾插删用 LinkedList……', 'explain', NULL),
 (1002, 'tutor_1002_01', 'start() 和 run() 的区别是什么？',
  'start() 创建新线程并由 JVM 回调 run()；直接调 run() 只是普通方法调用，不会新开线程……', 'explain', NULL);
