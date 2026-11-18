package org.example.backend.common;

/**
 * 所有业务接口共用的响应包。code = 0 表示成功，失败时 data 为 null。
 * HTTP 状态由控制器或全局异常处理器设置，不能只凭 code 判断请求是否成功。
 */
public class ApiResult<T> {

    private int code;
    private String message;
    private T data;

    public ApiResult() {
    }

    public ApiResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "操作成功", data);
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<>(0, "操作成功", null);
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        return new ApiResult<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
