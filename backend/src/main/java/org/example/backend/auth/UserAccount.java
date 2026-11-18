package org.example.backend.auth;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 用户账号实体（成员 1 scaffold）。
 */
@Data
public class UserAccount {

    private Long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String role;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
