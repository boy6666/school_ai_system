package com.eduagent.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应体。各服务列表接口统一返回此结构（配合 MyBatis-Plus Page 使用）。
 * 仅承载数据，不依赖 Web 层，可安全置于 common 模块。
 */
@Data
public class PageResult<T> implements Serializable {

    /** 总记录数 */
    private long total;

    /** 当前页数据 */
    private List<T> list;

    public static <T> PageResult<T> of(List<T> list, long total) {
        PageResult<T> r = new PageResult<>();
        r.setList(list);
        r.setTotal(total);
        return r;
    }
}
