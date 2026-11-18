package org.example.backend.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 当前会话用户（成员 1 提供给 2/3 的公共契约）。
 * 身份只从服务器会话取得，不接受请求参数传递。
 */
@Component
public class CurrentUser {

    /** 当前登录用户 ID；未登录抛 401/1003 */
    public Long id() {
        UserPrincipal principal = principal();
        if (principal == null) {
            throw BusinessException.unauthorized(ErrorCodes.AUTH_UNAUTHORIZED, "未登录或登录已过期");
        }
        return principal.getId();
    }

    /** 当前登录用户角色（ADMIN/OWNER）；未登录返回 null */
    public String role() {
        UserPrincipal principal = principal();
        return principal == null ? null : principal.getRole();
    }

    /** 要求当前登录用户为管理员；不是则 403/1004 */
    public Long requireAdminId() {
        UserPrincipal principal = principal();
        if (principal == null) {
            throw BusinessException.unauthorized(ErrorCodes.AUTH_UNAUTHORIZED, "未登录或登录已过期");
        }
        if (!"ADMIN".equals(principal.getRole())) {
            throw BusinessException.forbidden(ErrorCodes.AUTH_FORBIDDEN, "权限不足");
        }
        return principal.getId();
    }

    private UserPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal;
    }
}
