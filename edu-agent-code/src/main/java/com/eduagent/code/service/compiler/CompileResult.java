package com.eduagent.code.service.compiler;

import java.util.Map;

/**
 * 编译结果。
 * <ul>
 *   <li>成功：{@code ok=true}，{@code classes} 为「全限定类名 → 字节码」（如 {@code com.shop.Main}）；</li>
 *   <li>失败：{@code ok=false}，{@code error} 为诊断文本（javac 输出，含文件名/行号）。</li>
 * </ul>
 */
public record CompileResult(boolean ok, Map<String, byte[]> classes, String error) {

    public static CompileResult success(Map<String, byte[]> classes) {
        return new CompileResult(true, classes, null);
    }

    public static CompileResult fail(String error) {
        return new CompileResult(false, Map.of(), error);
    }
}
