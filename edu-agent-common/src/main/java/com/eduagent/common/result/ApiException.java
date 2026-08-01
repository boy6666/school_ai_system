package com.eduagent.common.result;

/**
 * 业务异常。配合各服务的 {@code @RestControllerAdvice} 统一转换为 {@link Result}。
 * 注意：全局异常处理器属于 Web 层，由各 Servlet 服务自行提供（common 不依赖 web）。
 */
public class ApiException extends RuntimeException {

    private final int code;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public ApiException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.code = errorCode.getCode();
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
