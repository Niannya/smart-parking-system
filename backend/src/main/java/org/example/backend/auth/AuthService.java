package org.example.backend.auth;

import org.example.backend.common.BusinessException;
import org.example.backend.common.CurrentUser;
import org.example.backend.common.ErrorCode;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    public record LoginResult(AccountView user, AuthPrincipal principal) {
    }

    private final UserAccountMapper accounts;
    private final PasswordEncoder passwords;
    private final String missingAccountHash;

    public AuthService(UserAccountMapper accounts, PasswordEncoder passwords) {
        this.accounts = accounts;
        this.passwords = passwords;
        // 用户名不存在时也执行一次 BCrypt 比对，避免明显的耗时差异。
        this.missingAccountHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AccountView register(String username, String password, String displayName) {
        String name = normalizeUsername(username);
        String display = requireText(displayName, "显示名称");
        validatePassword(password, true);
        if (accounts.insertOwner(name, passwords.encode(password), display) == 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }
        return accounts.findViewByUsername(name);
    }

    public LoginResult verifyLogin(String username, String password) {
        String name = normalizeUsername(username);
        validatePassword(password, false);
        UserAccountMapper.Credentials account = accounts.findCredentials(name);
        String hash = account == null ? missingAccountHash : account.passwordHash();
        if (!passwords.matches(password, hash) || account == null) {
            throw new BusinessException(ErrorCode.BAD_CREDENTIALS, "用户名或密码错误");
        }
        if (!"ACTIVE".equals(account.status())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已停用");
        }
        AuthPrincipal principal = new AuthPrincipal(account.id(), account.username(),
                account.role(), account.updatedAt());
        return new LoginResult(account.toView(), principal);
    }

    public AccountView currentAccount() {
        AccountView account = accounts.findViewById(CurrentUser.id());
        if (account == null) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "请先登录");
        }
        if (!"ACTIVE".equals(account.status())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已停用");
        }
        return account;
    }

    public List<AccountView> list(String role, String status) {
        if (role != null && !"ADMIN".equals(role) && !"OWNER".equals(role)) {
            throw invalid("角色只能是 ADMIN 或 OWNER");
        }
        if (status != null) {
            validateStatus(status);
        }
        return accounts.list(role, status);
    }

    @Transactional
    public AccountView changeStatus(Long id, String status) {
        if (id == null || id <= 0) {
            throw invalid("用户 ID 必须是正整数");
        }
        validateStatus(status);
        if ("DISABLED".equals(status) && id.equals(CurrentUser.id())) {
            throw new BusinessException(ErrorCode.SELF_DISABLE_FORBIDDEN, "不可停用当前管理员");
        }
        if (accounts.updateStatus(id, status) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "账号不存在");
        }
        return accounts.findViewById(id);
    }

    private static String normalizeUsername(String username) {
        String normalized = requireText(username, "用户名").toLowerCase(Locale.ROOT);
        return requireText(normalized, "用户名");
    }

    private static String requireText(String value, String field) {
        String trimmed = value == null ? "" : value.strip();
        if (trimmed.isEmpty() || trimmed.indexOf(0) >= 0
                || trimmed.codePointCount(0, trimmed.length()) > 50) {
            throw invalid(field + "须为 1–50 个字符");
        }
        return trimmed;
    }

    private static void validatePassword(String password, boolean registering) {
        if (password == null || password.isBlank() || password.indexOf(0) >= 0
                || password.length() > 72
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalid("密码不能为空、含空字符或超过 72 个 UTF-8 字节");
        }
        if (registering && password.codePointCount(0, password.length()) < 8) {
            throw invalid("密码至少 8 个字符");
        }
    }

    private static void validateStatus(String status) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw invalid("状态只能是 ACTIVE 或 DISABLED");
        }
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }
}
