"""LLM 代码修复器 —— DeepSeek 中文化代码 → 标准 Java。

由于文件编码问题无法直接在源码中写中文映射表，改用以下策略：
1. 检测代码块内是否包含中文字符
2. 如果包含，用正则规则修复常见的中文Java关键字模式
3. 对无法识别的中文字符，尝试用启发式规则处理
"""

import re


def _has_chinese(s: str) -> bool:
    """检测字符串是否包含中文字符（Unicode CJK范围）。"""
    return bool(re.search(r'[一-鿿㐀-䶿豈-﫿]', s))


def _fix_code_block(code: str) -> str:
    """修复单个代码块内的中文关键字。"""
    if not _has_chinese(code):
        return code

    # 规则1: 修复常见的中文→英文关键字（按长度降序）
    rules = [
        # ⚠️ 按长度降序！长词优先，防止"字符串"被"字符"和"串"分别替换
        ('系统.输出.打印行', 'System.out.println'),
        ('系统.输出.打印', 'System.out.print'),
        ('字符串', 'String'),
        ('双精度', 'double'),
        ('实现了', 'implements'),
        ('实现了', 'implements'),
        ('抽象类', 'abstract class'),
        ('实现', 'implements'),
        ('接口', 'interface'),
        ('扩展', 'extends'),
        ('抽象', 'abstract'),
        ('保护', 'protected'),
        ('静态', 'static'),
        ('最终', 'final'),
        ('私有', 'private'),
        ('布尔', 'boolean'),
        ('公共', 'public'),
        ('返回', 'return'),
        ('导入', 'import'),
        ('抛出', 'throws'),
        ('捕获', 'catch'),
        ('投掷', 'throw'),
        ('继续', 'continue'),
        ('中断', 'break'),
        ('切换', 'switch'),
        ('默认', 'default'),
        ('浮点', 'float'),
        ('超级', 'super'),
        ('短整', 'short'),
        ('长整', 'long'),
        ('字节', 'byte'),
        ('枚举', 'enum'),
        ('空值', 'null'),
        ('虚空', 'void'),
        ('尝试', 'try'),
        ('整型', 'int'),
        ('整数', 'int'),
        ('这个', 'this'),
        ('否则', 'else'),
        ('循环', 'for'),
        ('如果', 'if'),
        ('覆盖', '@Override'),
        ('重写', '@Override'),
        ('列表', 'List'),
        ('映射', 'Map'),
        ('集合', 'Set'),
        ('对象', 'Object'),
        ('输出', 'println'),
        ('打印', 'print'),
        ('字符', 'char'),
        ('假', 'false'),
        ('真', 'true'),
        ('空', 'void'),
        ('包', 'package'),
        ('新', 'new'),
        ('类', 'class'),
        ('当', 'while'),
    ]

    fixed = code
    for cn_pattern, en_word in rules:
        fixed = fixed.replace(cn_pattern, en_word)

    # 规则2: 修复被翻译成中文的常见类名模式
    # 例: 新动物() → new Animal(), 可飞行 → Flyable
    # 这类无法自动修复，但核心关键字已修复

    return fixed


def fix_code(text: str) -> str:
    """修复 LLM 输出代码块中的中文关键字。"""

    def replace_block(match):
        lang = match.group(1) or ""
        code = match.group(2)

        # 修复被翻译的语言标记
        if lang in ('爪娃', 'java', 'JAVA', 'Java'):
            lang = "java"

        fixed = _fix_code_block(code)
        return f"```{lang}\n{fixed}\n```"

    # 匹配所有代码块 ```...```
    text = re.sub(
        r'```(\S*)\s*\n(.*?)```',
        replace_block,
        text,
        flags=re.DOTALL,
    )

    return text
