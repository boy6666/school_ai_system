package com.eduagent.resource.dto;

import lombok.Data;
import java.util.Map;

@Data
public class KbStatsResp {
    private long totalCorpus;
    private long totalChunks;
    private Map<String, Long> statusCounts;
}
