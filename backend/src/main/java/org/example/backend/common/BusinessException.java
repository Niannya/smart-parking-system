package org.example.backend.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常：携带稳定错误码与 HTTP 状态，由 GlobalExceptionHandler 统一包装为 ApiResult。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus httpStatus;

    public BusinessException(HttpStatus httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public static BusinessException badRequest(int code, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static BusinessException unauthorized(int code, String message) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static BusinessException forbidden(int code, String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, code, message);
    }

    public static BusinessException notFound(int code, String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, code, message);
    }

    public static BusinessException conflict(int code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
