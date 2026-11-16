package org.example.backend.common;

/**
 * 业务异常：表示「用户操作不合法」这类可预期错误，由 {@link GlobalExceptionHandler} 转成
 * 统一响应返回给前端，不打印完整堆栈。
 *
 * <p>错误码目前按模块分段使用：4001~4099 停车区域，4101~4199 停车位，
 * 4201~4299 车主车辆，4301~4399 车辆入场。成员 1 如统一规划错误码，按新表调整即可。</p>
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
