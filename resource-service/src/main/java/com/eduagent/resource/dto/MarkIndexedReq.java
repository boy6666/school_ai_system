package com.eduagent.resource.dto;

import lombok.Data;
import java.util.List;

@Data
public class MarkIndexedReq {
    private List<Long> ids;
}
