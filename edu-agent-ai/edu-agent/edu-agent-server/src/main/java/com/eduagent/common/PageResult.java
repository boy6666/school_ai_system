package com.eduagent.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private List<T> records;

    // alias for frontend
    public List<T> getList() { return records; }
    private long total;
    private int page;
    private int pageSize;
}
