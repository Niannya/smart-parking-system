package org.example.backend.auth;

import lombok.RequiredArgsConstructor;
import org.example.backend.common.BusinessException;
import org.example.backend.common.ErrorCodes;
import org.example.backend.common.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 按用户名加载登录主体（成员 1 scaffold）。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserAccountMapper userAccountMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountMapper.selectByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        return toPrincipal(account);
    }

    public UserPrincipal toPrincipal(UserAccount account) {
        return new UserPrincipal(account.getId(), account.getUsername(),
                account.getDisplayName(), account.getRole(), account.getStatus(),
                account.getPasswordHash());
    }

    /** 供 AccountStatusFilter 复用的停用判定 */
    public void checkActive(Long userId) {
        String status = userAccountMapper.selectStatusById(userId);
        if (status == null) {
            throw BusinessException.unauthorized(ErrorCodes.AUTH_UNAUTHORIZED, "账号不存在，请重新登录");
        }
        if (!"ACTIVE".equals(status)) {
            throw BusinessException.unauthorized(ErrorCodes.AUTH_ACCOUNT_DISABLED, "账号已停用");
        }
    }
}
