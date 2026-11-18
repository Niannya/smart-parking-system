package org.example.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求：只创建 OWNER/ACTIVE；出现 role 字段直接 400（Jackson 未知字段拒绝）。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 50, message = "用户名长度须在 1-50 字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度须在 8-64 字符")
    private String password;

    @NotBlank(message = "显示名不能为空")
    @Size(min = 1, max = 50, message = "显示名长度须在 1-50 字符")
    private String displayName;
}
