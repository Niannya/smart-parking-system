package org.example.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCodes;
import org.example.backend.common.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务（成员 1 scaffold）：注册只建 OWNER；登录建立服务器会话；
 * 手动登录须显式保存 SecurityContext 到会话（Spring Security 官方会话管理要求）。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountMapper userAccountMapper;
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository =
            new DelegatingSecurityContextRepository(
                    new HttpSessionSecurityContextRepository(),
                    new RequestAttributeSecurityContextRepository());

    @Transactional
    public UserDto register(RegisterRequest request) {
        String username = request.getUsername().trim().toLowerCase();
        if (userAccountMapper.selectByUsername(username).isPresent()) {
            throw BusinessException.conflict(ErrorCodes.AUTH_USERNAME_EXISTS, "用户名已存在");
        }
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPasswordHash(new BCryptPasswordEncoder().encode(request.getPassword()));
        account.setDisplayName(request.getDisplayName().trim());
        account.setRole("OWNER");
        account.setStatus("ACTIVE");
        userAccountMapper.insert(account);
        return toDto(account);
    }

    /** 登录并建立会话；旧会话（SESSION 失效策略）由仓库覆盖 */
    public UserDto login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String username = request.getUsername().trim().toLowerCase();
        UserAccount account = userAccountMapper.selectByUsername(username)
                .orElseThrow(() -> BusinessException.unauthorized(ErrorCodes.AUTH_BAD_CREDENTIALS, "用户名或密码错误"));
        if (!"ACTIVE".equals(account.getStatus())) {
            throw BusinessException.unauthorized(ErrorCodes.AUTH_ACCOUNT_DISABLED, "账号已停用");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, request.getPassword()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return toDto(account);
    }

    public UserDto me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw BusinessException.unauthorized(ErrorCodes.AUTH_UNAUTHORIZED, "未登录或登录已过期");
        }
        return toDto(userAccountMapper.selectById(principal.getId())
                .orElseThrow(() -> BusinessException.unauthorized(ErrorCodes.AUTH_UNAUTHORIZED, "账号不存在，请重新登录")));
    }

    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
        securityContextRepository.saveContext(emptyContext, httpRequest, httpResponse);
    }

    private UserDto toDto(UserAccount account) {
        return new UserDto(account.getId(), account.getUsername(), account.getDisplayName(),
                account.getRole(), account.getStatus());
    }
}
