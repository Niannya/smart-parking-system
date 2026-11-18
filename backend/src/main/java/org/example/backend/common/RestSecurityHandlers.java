package org.example.backend.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全过滤器产生的 401/403 也使用统一 JSON 外壳（01 契约第 4 节）。
 * CSRF 缺失/无效为 1006，其余访问拒绝为 1004。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED,
                ApiResult.fail(ErrorCodes.AUTH_UNAUTHORIZED, "未登录或登录已过期"));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        int code = ErrorCodes.AUTH_FORBIDDEN;
        String message = "权限不足";
        Throwable cause = accessDeniedException.getCause();
        if (accessDeniedException instanceof MissingCsrfTokenException
                || accessDeniedException instanceof InvalidCsrfTokenException
                || cause instanceof CsrfException) {
            code = ErrorCodes.AUTH_CSRF_INVALID;
            message = "CSRF token 缺失或无效";
        }
        write(response, HttpServletResponse.SC_FORBIDDEN, ApiResult.fail(code, message));
    }

    private void write(HttpServletResponse response, int status, ApiResult<Void> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
