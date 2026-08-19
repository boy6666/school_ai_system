package com.eduagent.code.service.compiler;

/**
 * 一个待编译/待检查的源文件。name 可为纯文件名（Main.java）或带包路径（com/shop/Main.java）。
 */
public record SourceFile(String name, String source) {
}
