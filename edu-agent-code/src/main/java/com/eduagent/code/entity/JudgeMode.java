package com.eduagent.code.entity;

/**
 * 判题类型（共识 3）：预留两档。
 * <ul>
 *   <li>{@link #IO}     标准 I/O 比对：编译后运行，stdout 与 expectedOutput 比对（主流）；</li>
 *   <li>{@link #HARNESS} 隐藏测试：题目藏测试，判指定类/方法的行为（老师要求必须用某实现时可选）。</li>
 * </ul>
 */
public enum JudgeMode {
    IO,
    HARNESS
}
