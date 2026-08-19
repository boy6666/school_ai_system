package com.eduagent.code.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 提交的一个源文件（共识 1：可多文件、可多包、可不打包）。
 * name 如 {@code Dog.java} / {@code com/shop/Main.java}（包路径用 / 或 . 分隔均可）。
 */
@Data
public class CodeFile {

    @NotBlank(message = "文件名不能为空")
    private String name;

    @NotBlank(message = "源码不能为空")
    private String sourceCode;
}
