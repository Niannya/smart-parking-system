package org.example.backend.support;

/**
 * 当前登录用户的只读视图：用户 ID + 角色。
 *
 * <p>归属说明：真正的 {@code CurrentUser} 由成员 1 在 {@code common} 包提供。
 * 本类是成员 2 模块内部的适配视图，只承载业务需要的两个值，不复制任何鉴权逻辑，
 * 也不从请求体、请求头读取身份。成员 1 的认证合并后，只需把认证结果转换成
 * {@link CurrentUserView} 交给 {@link CurrentUserProvider}（见该类说明）。</p>
 */
public record CurrentUserView(Long id, String role) {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_OWNER = "OWNER";

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public boolean isOwner() {
        return ROLE_OWNER.equals(role);
    }
}
