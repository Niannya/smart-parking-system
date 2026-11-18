package org.example.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.common.ApiResult;
import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCodes;
import org.example.backend.common.UserPrincipal;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 每次受保护请求核验账号仍为 ACTIVE：已停用账号的旧会话不得继续访问（01 契约第 2 节）。
 * 位于 SecurityContextHolderFilter 之后，此时会话身份已加载。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserAccountMapper userAccountMapper;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            Long userId = principal.getId();
            String status = userAccountMapper.selectStatusById(userId);
            if (status == null) {
                SecurityContextHolder.clearContext();
                write(response, 401, ErrorCodes.AUTH_UNAUTHORIZED, "账号不存在，请重新登录");
                return;
            }
            if (!"ACTIVE".equals(status)) {
                SecurityContextHolder.clearContext();
                write(response, 401, ErrorCodes.AUTH_ACCOUNT_DISABLED, "账号已停用");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void write(HttpServletResponse response, int status, int code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResult.fail(code, message));
    }
}
