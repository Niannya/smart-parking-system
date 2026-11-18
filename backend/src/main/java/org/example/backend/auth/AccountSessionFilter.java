package org.example.backend.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.backend.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public final class AccountSessionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccountSessionFilter.class);

    private final RequestMatcher publicEndpoints;
    private final UserAccountMapper accounts;
    private final SecurityContextLogoutHandler logoutHandler;
    private final AuthFailureWriter failureWriter;

    public AccountSessionFilter(RequestMatcher publicEndpoints, UserAccountMapper accounts,
                                SecurityContextLogoutHandler logoutHandler, AuthFailureWriter failureWriter) {
        this.publicEndpoints = publicEndpoints;
        this.accounts = accounts;
        this.logoutHandler = logoutHandler;
        this.failureWriter = failureWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return publicEndpoints.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            chain.doFilter(request, response);
            return;
        }

        UserAccountMapper.SessionState state;
        try {
            state = accounts.findSessionState(principal.id());
        } catch (DataAccessException e) {
            log.error("查询会话对应的账号状态失败", e);
            failureWriter.write(response, ErrorCode.UNKNOWN_ERROR, "系统繁忙，请稍后重试");
            return;
        }

        if (state != null && !"ACTIVE".equals(state.status())) {
            logoutHandler.logout(request, response, authentication);
            failureWriter.write(response, ErrorCode.ACCOUNT_DISABLED, "账号已停用");
            return;
        }
        // 比较版本，确保账号先停用再启用也不会恢复旧会话。
        if (state == null || !principal.role().equals(state.role())
                || !principal.accountVersion().isEqual(state.updatedAt())) {
            logoutHandler.logout(request, response, authentication);
            failureWriter.write(response, ErrorCode.UNAUTHENTICATED, "会话已失效，请重新登录");
            return;
        }
        chain.doFilter(request, response);
    }
}
