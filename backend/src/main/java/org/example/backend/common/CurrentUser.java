package org.example.backend.common;

import org.example.backend.auth.AuthPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 业务服务从认证上下文获取操作者，不从请求字段读取用户 ID 或角色。 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        return principal().id();
    }

    public static String role() {
        return principal().role();
    }

    private static AuthPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            return principal;
        }
        throw new BusinessException(ErrorCode.UNAUTHENTICATED, "请先登录");
    }
}
