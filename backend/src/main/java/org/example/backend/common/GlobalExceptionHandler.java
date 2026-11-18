package org.example.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理：参数错误 400、未登录 401、无权 403、业务冲突 409、未知错误 500。
 * 未知错误正文只显示通用提示，具体异常留在服务端日志（01 契约第 4 节）。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按异常携带的 HTTP 状态与错误码返回 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getHttpStatus())
                .body(ApiResult.fail(e.getCode(), e.getMessage()));
    }

    /** Bean Validation / 绑定错误 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResult<Void>> handleValidation(Exception e) {
        String message = "请求参数错误";
        if (e instanceof MethodArgumentNotValidException ex && ex.getBindingResult().getFieldError() != null) {
            message = ex.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(ApiResult.fail(ErrorCodes.BAD_REQUEST, message));
    }

    /** 缺少必填参数、路径参数类型错误（如 ID 非正整数）、未知 JSON 字段 */
    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResult<Void>> handleRequestParam(Exception e) {
        return ResponseEntity.badRequest().body(ApiResult.fail(ErrorCodes.BAD_REQUEST, "请求参数错误或格式不正确"));
    }

    /** 安全过滤器之外的访问拒绝（过滤器内的由 RestAccessDeniedHandler 处理） */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResult.fail(ErrorCodes.AUTH_FORBIDDEN, "权限不足"));
    }

    /** 认证异常（过滤器内的由 RestAuthenticationEntryPoint 处理） */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResult<Void>> handleAuthentication(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResult.fail(ErrorCodes.AUTH_BAD_CREDENTIALS, "用户名或密码错误"));
    }

    /** 未知错误：不泄露 SQL、环境变量或堆栈 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnknown(HttpServletRequest request, Exception e) {
        log.error("未处理异常: {} {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(ErrorCodes.UNKNOWN, "系统繁忙，请稍后重试"));
    }
}
