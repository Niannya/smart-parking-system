package org.example.backend.common;

import org.springframework.http.HttpStatus;

/** 业务异常携带稳定错误码及对应 HTTP 状态。 */
public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus status;

    /**
     * 兼容 PR #6 的区域代码；成员 2 收口时应迁移到 ErrorCode 构造器。
     * 旧区域代码的数字与正式契约有冲突，因此此构造器暂按参数错误处理。
     */
    @Deprecated
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.status = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.status = errorCode.getStatus();
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
