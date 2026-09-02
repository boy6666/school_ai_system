package com.eduagent.code.service.compiler;

/**
 * 把落库的单列 {@code source_code} 还原回多文件列表（见 SubmissionServiceImpl 的 {@code //===== FILE: <name>} 拼接格式）。
 * 兼兼容无标记的单文件形态（退化为一个文件，用 className 造文件名）。
 */
public final class SourceSplitter {

    private static final String MARKER = "//===== FILE: ";

    private SourceSplitter() {
    }

    public static java.util.List<SourceFile> split(String sourceCode, String entryClassName) {
        java.util.List<SourceFile> files = new java.util.ArrayList<>();
        String[] chunks = sourceCode.split("(?=\\Q" + MARKER + "\\E)", -1);
        for (String chunk : chunks) {
            if (chunk.startsWith(MARKER)) {
                int nl = chunk.indexOf('\n');
                String name = chunk.substring(MARKER.length(), nl < 0 ? chunk.length() : nl);
                String src = nl < 0 ? "" : chunk.substring(nl + 1);
                files.add(new SourceFile(name, src));
            } else if (!chunk.isBlank()) {
                // 无标记形态：整个当做一个文件，文件名取入口类
                files.add(new SourceFile(safeEntryName(entryClassName) + ".java", chunk));
            }
        }
        return files;
    }

    /** 去掉包路径/非法字符，仅保留简单类名片段，避免文件名注入 */
    private static String safeEntryName(String className) {
        if (className == null || className.isBlank()) {
            return "Main";
        }
        int dot = Math.max(className.lastIndexOf('.'), className.lastIndexOf('/'));
        String simple = dot >= 0 ? className.substring(dot + 1) : className;
        return simple.matches("[A-Za-z_$][A-Za-z0-9_$]*") ? simple : "Main";
    }
}
