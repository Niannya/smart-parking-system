package org.example.backend.auth;

import org.springframework.security.core.AuthenticatedPrincipal;

import java.io.Serializable;
import java.time.OffsetDateTime;

/** 在服务端会话中保存的身份，不保存密码。 */
public record AuthPrincipal(Long id, String username, String role, OffsetDateTime accountVersion)
        implements AuthenticatedPrincipal, Serializable {

    @Override
    public String getName() {
        return username;
    }
}
