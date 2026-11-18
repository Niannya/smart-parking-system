package org.example.backend.common;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 登录主体：携带用户 ID 与角色，供 CurrentUser 取用。
 * password 为库中 bcrypt 哈希，仅供 DaoAuthenticationProvider 校验，不对外序列化。
 * 账号停用后非锁定/过期，由 AccountStatusFilter 每次请求核验。
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String displayName;
    private final String role;
    private final String status;
    private final String password;

    public UserPrincipal(Long id, String username, String displayName, String role, String status,
                         String password) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(status);
    }
}
