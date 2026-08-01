package com.eduagent.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体。所有接口返回均包裹此类，由《契约对齐决议》C4 约定 wire 为 camelCase。
 */
@Data
public class Result<T> implements Serializable {

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(0);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }
}
