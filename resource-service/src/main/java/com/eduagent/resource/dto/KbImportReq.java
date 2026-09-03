package com.eduagent.resource.dto;

import lombok.Data;
import java.util.List;

@Data
public class KbImportReq {
    private List<CorpusItem> items;

    @Data
    public static class CorpusItem {
        private String content;
        private String source;
        private String type;      // 如 "java_note"
        private String metadata;  // JSON 字符串
    }
}
