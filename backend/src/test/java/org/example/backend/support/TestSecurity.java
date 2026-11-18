package org.example.backend.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.example.backend.common.UserPrincipal;

import java.util.List;

/**
 * 测试期安全上下文工具：按约定注入 UserPrincipal（成员 3 测试用）。
 */
public final class TestSecurity {

    private TestSecurity() {
    }

    public static void setAdmin(Long adminId) {
        setUser(adminId, "ADMIN");
    }

    public static void setOwner(Long ownerId) {
        setUser(ownerId, "OWNER");
    }

    public static void setUser(Long id, String role) {
        // 测试不走 DaoAuthenticationProvider，password 传 null 即可
        UserPrincipal principal = new UserPrincipal(id, role.equals("ADMIN") ? "admin" : "owner",
                "测试用户", role, "ACTIVE", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
