package org.example.backend.auth;

/** 对外账号 DTO，不包含密码哈希或内部会话版本。 */
public record AccountView(Long id, String username, String displayName, String role, String status) {
}
