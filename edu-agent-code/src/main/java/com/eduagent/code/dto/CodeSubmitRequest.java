package com.eduagent.code.dto;

import com.eduagent.code.entity.JudgeMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 判分提交请求（受理口入参）。
 * <p>共识 1：主形态为 {@code files[] + className}（从 0 写的多文件、可多包、可不打包）。
 * <p>{@code sourceCode} 保留以兼容《契约对齐决议》C1 冻结的单文件形态 {@code {studentId, assignmentId,
 * assignmentItemId, language, sourceCode, expectedOutput, className}}；当 files 非空时以 files 为准。
 * <p>{@code studentId} 缺省时回退到 {@link com.eduagent.common.security.AuthContext}（学生直连推送）。
 * <p>{@code mode} 见 {@link JudgeMode}：预留判题类型 (a) 标准 I/O 比对 / (b) 隐藏测试。
 */
@Data
public class CodeSubmitRequest {

    /** 学生 id；缺省时取 AuthContext（teacher 代提交 / 学生直连两种来源都覆盖） */
    private Long studentId;

    private Long assignmentId;

    private Long assignmentItemId;

    @NotBlank(message = "语言不能为空")
    @Size(max = 16, message = "语言标识过长")
    private String language = "java";

    /** 共识 1：多文件源码（优先）；为空时回退单文件 sourceCode */
    private List<CodeFile> files;

    /** 入口类名（可简单类名如 Test1，不强制 Main） */
    @NotBlank(message = "入口类名 className 不能为空")
    private String className;

    /** 兼容单文件形态的源码；仅当 files 为空时使用 */
    private String sourceCode;

    /** 判题类型 (a) 标准 I/O 比对，比对目标 */
    private String expectedOutput;

    /** 判题类型：(a) 默认 IO / (b) HARNESS 预留 */
    private JudgeMode mode = JudgeMode.IO;
}
