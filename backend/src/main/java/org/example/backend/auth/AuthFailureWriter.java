package org.example.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.example.backend.common.ApiResult;
import org.example.backend.common.ErrorCode;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 安全过滤链不经过 MVC 异常处理，仍使用同一响应包。 */
public final class AuthFailureWriter {

    private final ObjectMapper objectMapper;

    public AuthFailureWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, ErrorCode error, String message) throws IOException {
        response.setStatus(error.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-store");
        objectMapper.writeValue(response.getOutputStream(), ApiResult.fail(error.getCode(), message));
    }
}
