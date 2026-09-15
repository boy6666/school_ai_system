package com.eduagent.learning.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * 分页结果。common 模块未提供 PageResult，按《契约对齐决议》在 learning 内自建。
 * list 别名兼容前端旧字段（ getList() == getRecords() ）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private List<T> records;
    private long total;
    private long page;
    private long pageSize;

    /** 前端旧契约兼容：部分页面读 list 字段 */
    public List<T> getList() {
        return records;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long page, long pageSize) {
        return new PageResult<>(records, total, page, pageSize);
    }

    public static <T> PageResult<T> empty(long page, long pageSize) {
        return new PageResult<>(List.of(), 0, page, pageSize);
    }

    /** MyBatis-Plus Page → PageResult，元素做 VO 映射 */
    public static <E, T> PageResult<T> from(Page<E> mpPage, Function<E, T> mapper) {
        List<T> mapped = mpPage.getRecords().stream().map(mapper).toList();
        return new PageResult<>(mapped, mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize());
    }
}
