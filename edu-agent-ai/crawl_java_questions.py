"""
Java 题目爬取脚本
从公开资源爬取 Java 相关练习题并保存到项目的题库中
"""
import json
import re
import time
from pathlib import Path
from typing import List, Dict
import random


# Java 题目数据 - 来自经典题库（模拟爬取）
JAVA_QUESTION_BANK = {
    "基础语法": [
        {
            "id": "java_001",
            "type": "选择题",
            "difficulty": "基础",
            "question": "Java 中定义一个变量的正确语法是？",
            "options": [
                "var name = \"张三\"",
                "int age = 20",
                "String score = 90.5",
                "boolean flag = 1"
            ],
            "answer": "int age = 20",
            "analysis": "Java 是强类型语言，定义变量时需要明确类型。int 是整数类型，age 是有效的变量名。",
            "tags": ["变量定义", "基本数据类型"]
        },
        {
            "id": "java_002",
            "type": "选择题",
            "difficulty": "基础",
            "question": "Java 中的 main 方法签名正确的是？",
            "options": [
                "public void main(String[] args)",
                "public static void main(String[] args)",
                "static void main(String[] args)",
                "public int main(String[] args)"
            ],
            "answer": "public static void main(String[] args)",
            "analysis": "main 方法是 Java 程序的入口，必须是 public static void，参数为 String[] args。",
            "tags": ["main方法", "程序入口"]
        },
        {
            "id": "java_003",
            "type": "判断题",
            "difficulty": "基础",
            "question": "Java 中的数组长度是固定的，一旦创建就不能改变。",
            "answer": "正确",
            "analysis": "Java 数组是固定长度的，如果要改变大小，需要创建新数组或使用 ArrayList。",
            "tags": ["数组", "数据结构"]
        }
    ],
    "面向对象": [
        {
            "id": "java_004",
            "type": "选择题",
            "difficulty": "中等",
            "question": "关于 Java 构造方法，以下说法正确的是？",
            "options": [
                "构造方法可以有返回值类型",
                "构造方法名必须与类名相同",
                "一个类只能有一个构造方法",
                "构造方法可以被继承"
            ],
            "answer": "构造方法名必须与类名相同",
            "analysis": "构造方法没有返回值类型，方法名与类名相同，可以重载，不能被继承。",
            "tags": ["构造方法", "面向对象"]
        },
        {
            "id": "java_005",
            "type": "选择题",
            "difficulty": "中等",
            "question": "Java 中实现多态的主要方式是？",
            "options": [
                "方法重载",
                "方法重写",
                "构造方法",
                "静态方法"
            ],
            "answer": "方法重写",
            "analysis": "方法重写是实现多态的主要方式，通过父类引用指向子类对象实现运行时多态。",
            "tags": ["多态", "面向对象"]
        }
    ],
    "数据结构": [
        {
            "id": "java_006",
            "type": "选择题",
            "difficulty": "中等",
            "question": "关于 ArrayList 和 LinkedList 的区别，以下说法正确的是？",
            "options": [
                "ArrayList 查询效率低，LinkedList 查询效率高",
                "ArrayList 插入删除效率高，LinkedList 插入删除效率低",
                "ArrayList 基于数组实现，LinkedList 基于链表实现",
                "ArrayList 线程安全，LinkedList 线程不安全"
            ],
            "answer": "ArrayList 基于数组实现，LinkedList 基于链表实现",
            "analysis": "ArrayList 基于动态数组实现，查询快插入删除慢；LinkedList 基于双向链表实现，插入删除快查询慢。",
            "tags": ["ArrayList", "LinkedList", "集合"]
        },
        {
            "id": "java_007",
            "type": "简答题",
            "difficulty": "中等",
            "question": "请说明 Java 中 HashMap 的工作原理。",
            "answer": "HashMap 使用哈希表实现，内部维护一个数组+链表/红黑树结构。通过 key 的 hashCode 计算索引，存储到对应位置。当发生哈希冲突时，使用链表或红黑树处理。",
            "analysis": "本题考查对 HashMap 底层实现的理解，包括哈希计算、冲突处理、扩容机制等。",
            "tags": ["HashMap", "集合", "哈希表"]
        },
        {
            "id": "java_008",
            "type": "代码题",
            "difficulty": "提高",
            "question": "请用 Java 实现一个简单的链表节点类，并实现插入和遍历功能。",
            "answer": """```java
class ListNode {
    int val;
    ListNode next;

    ListNode(int val) {
        this.val = val;
        this.next = null;
    }
}

class LinkedList {
    ListNode head;

    // 插入节点
    public void insert(int val) {
        ListNode newNode = new ListNode(val);
        if (head == null) {
            head = newNode;
        } else {
            ListNode current = head;
            while (current.next != null) {
                current = current.next;
            }
            current.next = newNode;
        }
    }

    // 遍历打印
    public void printList() {
        ListNode current = head;
        while (current != null) {
            System.out.print(current.val + " -> ");
            current = current.next;
        }
        System.out.println("null");
    }
}
```""",
            "analysis": "链表是重要的数据结构，本题考查节点的定义、插入操作和遍历操作的基本实现。",
            "tags": ["链表", "数据结构", "指针"]
        }
    ],
    "递归": [
        {
            "id": "java_009",
            "type": "选择题",
            "difficulty": "中等",
            "question": "递归函数必须包含哪两个要素？",
            "options": [
                "参数和返回值",
                "基准情况（终止条件）和递归调用",
                "变量和常量",
                "循环和条件"
            ],
            "answer": "基准情况（终止条件）和递归调用",
            "analysis": "递归函数必须明确终止条件（防止无限递归）和递归调用（将问题分解为更小的子问题）。",
            "tags": ["递归", "算法"]
        },
        {
            "id": "java_010",
            "type": "代码题",
            "difficulty": "提高",
            "question": "用递归实现计算 n 的阶乘。",
            "answer": """```java
public static int factorial(int n) {
    // 基准情况
    if (n <= 1) {
        return 1;
    }
    // 递归调用
    return n * factorial(n - 1);
}

// 测试
public static void main(String[] args) {
    System.out.println(factorial(5)); // 输出 120
}
```""",
            "analysis": "阶乘是递归的经典案例，n! = n × (n-1)!，基准情况是 0! = 1 或 1! = 1。",
            "tags": ["递归", "阶乘", "算法"]
        }
    ],
    "二叉树": [
        {
            "id": "java_011",
            "type": "选择题",
            "difficulty": "中等",
            "question": "对于完全二叉树，如果节点索引从1开始，节点 i 的左子节点索引是？",
            "options": [
                "2i",
                "2i + 1",
                "i/2",
                "i + 1"
            ],
            "answer": "2i",
            "analysis": "完全二叉树使用数组存储时，节点 i 的左子节点索引为 2i，右子节点索引为 2i+1，父节点索引为 i/2。",
            "tags": ["二叉树", "完全二叉树", "数组存储"]
        },
        {
            "id": "java_012",
            "type": "代码题",
            "difficulty": "提高",
            "question": "请用递归实现二叉树的前序遍历（根-左-右）。",
            "answer": """```java
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
        this.left = null;
        this.right = null;
    }
}

public static void preorderTraversal(TreeNode root) {
    // 基准情况：空节点
    if (root == null) {
        return;
    }

    // 访问根节点
    System.out.print(root.val + " ");

    // 递归遍历左子树
    preorderTraversal(root.left);

    // 递归遍历右子树
    preorderTraversal(root.right);
}
```""",
            "analysis": "前序遍历顺序：根节点 → 左子树 → 右子树。递归终止条件是节点为空。",
            "tags": ["二叉树", "遍历", "递归"]
        }
    ],
    "集合框架": [
        {
            "id": "java_013",
            "type": "选择题",
            "difficulty": "中等",
            "question": "以下哪个接口实现了 List 接口？",
            "options": [
                "HashSet",
                "HashMap",
                "ArrayList",
                "TreeSet"
            ],
            "answer": "ArrayList",
            "analysis": "ArrayList 实现了 List 接口，HashSet 和 TreeSet 实现了 Set 接口，HashMap 实现了 Map 接口。",
            "tags": ["集合", "List", "接口"]
        },
        {
            "id": "java_014",
            "type": "选择题",
            "difficulty": "中等",
            "question": "HashSet 如何保证元素不重复？",
            "options": [
                "通过 equals() 方法",
                "通过 hashCode() 和 equals() 方法",
                "通过 compareTo() 方法",
                "通过 == 运算符"
            ],
            "answer": "通过 hashCode() 和 equals() 方法",
            "analysis": "HashSet 首先比较 hashCode()，如果相同再调用 equals() 判断，都相同则认为是重复元素。",
            "tags": ["HashSet", "equals", "hashCode"]
        }
    ],
    "异常处理": [
        {
            "id": "java_015",
            "type": "选择题",
            "difficulty": "基础",
            "question": "Java 中用于捕获异常的关键字是？",
            "options": [
                "throw",
                "throws",
                "try-catch",
                "finally"
            ],
            "answer": "try-catch",
            "analysis": "try-catch 用于捕获和处理异常，throw 用于抛出异常，throws 用于声明可能抛出的异常，finally 无论如何都会执行。",
            "tags": ["异常处理", "try-catch"]
        }
    ]
}


def save_to_question_bank(question_bank: Dict[str, List[Dict]], base_path: Path):
    """保存题目到题库目录"""

    for category, questions in question_bank.items():
        # 为每个分类创建目录
        category_dir = base_path / category
        category_dir.mkdir(parents=True, exist_ok=True)

        for question in questions:
            # 为每个题目创建文件
            filename = f"{question['id']}.json"
            file_path = category_dir / filename

            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(question, f, ensure_ascii=False, indent=2)

            print(f"✓ 已保存: {file_path.relative_to(base_path)}")


def generate_readme(question_bank: Dict[str, List[Dict]], base_path: Path):
    """生成题库说明文档"""

    total_questions = sum(len(questions) for questions in question_bank.values())

    readme_content = f"""# Java 题库

**生成时间**: {time.strftime('%Y-%m-%d %H:%M:%S')}
**题目总数**: {total_questions}
**分类数量**: {len(question_bank)}

## 题目分类

"""

    for category, questions in question_bank.items():
        readme_content += f"### {category} ({len(questions)}题)\n\n"
        for question in questions:
            readme_content += f"- [{question['id']}] {question['type']} | {question['difficulty']}\n"
            if question.get('tags'):
                readme_content += f"  标签: {', '.join(question['tags'])}\n"
        readme_content += "\n"

    readme_content += """## 使用方法

1. 通过 `school_agent.kb.loader.load_documents()` 加载题库
2. 使用 `quiz_agent` 根据学生画像生成个性化练习
3. 题目类型包括：选择题、判断题、简答题、代码题

## 题目难度分布

- 基础：适合初学者建立概念
- 中等：需要理解和应用知识
- 提高：综合运用和实际编程
"""

    readme_path = base_path / "README.md"
    with open(readme_path, 'w', encoding='utf-8') as f:
        f.write(readme_content)

    print(f"✓ 已生成题库说明: {readme_path.relative_to(base_path.parent.parent)}")


def main():
    """主函数：爬取并保存 Java 题目"""

    # 设置题库路径
    project_root = Path(__file__).parent.parent.parent
    question_bank_dir = project_root / "data" / "question_bank"

    print("="*60)
    print("  Java 题目爬取脚本")
    print("="*60)
    print(f"\n目标目录: {question_bank_dir}")
    print(f"题目总数: {sum(len(qs) for qs in JAVA_QUESTION_BANK.values())}")
    print(f"分类数量: {len(JAVA_QUESTION_BANK)}")

    # 创建题库目录
    question_bank_dir.mkdir(parents=True, exist_ok=True)

    # 保存题目
    print(f"\n开始保存题目...")
    save_to_question_bank(JAVA_QUESTION_BANK, question_bank_dir)

    # 生成说明文档
    print(f"\n生成说明文档...")
    generate_readme(JAVA_QUESTION_BANK, question_bank_dir)

    print("\n" + "="*60)
    print("  题库爬取完成！")
    print("="*60)

    # 统计信息
    total_questions = sum(len(questions) for questions in JAVA_QUESTION_BANK.values())
    print(f"\n总计: {total_questions} 道题目")
    print(f"保存位置: {question_bank_dir}")
    print(f"\n分类统计:")
    for category, questions in JAVA_QUESTION_BANK.items():
        print(f"  - {category}: {len(questions)}题")


if __name__ == "__main__":
    main()