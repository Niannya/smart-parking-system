package org.example.backend.common;

/**
 * 统一接口响应结构（临时版）。
 *
 * <p>项目约定「统一接口响应结构」由成员 1 负责，本类是为了让本模块可以独立开发和联调先行落地。
 * 成员 1 提交统一响应后，应整体替换本类并同步调整 Controller 的返回类型，业务代码不需要改。</p>
 *
 * <p>约定：code = 0 表示成功；非 0 为业务错误码，message 为可直接展示给用户的中文提示。</p>
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
