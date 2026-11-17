package org.example.backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理（临时版，待成员 1 统一）。
 *
 * <p>关键约束：任何异常都不能把 SQL 语句、表名、数据库连接信息、堆栈返回给前端
 * （需求分析 11.3 节）。业务异常返回可读中文；未知异常统一返回「系统繁忙」并在服务端日志里留痕。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusiness(BusinessException e) {
        log.info("业务校验未通过: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<ApiResult<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.info("参数不合法: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.fail(4000, "请求参数不合法"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnknown(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(5000, "系统繁忙，请稍后重试"));
    }
}
