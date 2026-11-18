package org.example.backend.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户响应：绝不返回 passwordHash。
 */
@Data
@AllArgsConstructor
public class UserDto {

    private Long id;
    private String username;
    private String displayName;
    private String role;
    private String status;
}
