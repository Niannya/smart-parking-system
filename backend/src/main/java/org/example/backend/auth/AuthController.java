package org.example.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.backend.common.ApiResult;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record RegisterRequest(String username, String password, String displayName) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record CsrfReply(String token, String headerName) {
    }

    private final AuthService auth;
    private final SecurityContextRepository contextRepository;
    private final SessionAuthenticationStrategy loginSessionStrategy;
    private final SecurityContextLogoutHandler logoutHandler;

    public AuthController(AuthService auth, SecurityContextRepository contextRepository,
                          SessionAuthenticationStrategy loginSessionStrategy,
                          SecurityContextLogoutHandler logoutHandler) {
        this.auth = auth;
        this.contextRepository = contextRepository;
        this.loginSessionStrategy = loginSessionStrategy;
        this.logoutHandler = logoutHandler;
    }

    @GetMapping("/csrf")
    public ApiResult<CsrfReply> csrf(@RequestAttribute("_csrf") CsrfToken token) {
        return ApiResult.ok(new CsrfReply(token.getToken(), token.getHeaderName()));
    }

    @PostMapping("/register")
    public ApiResult<AccountView> register(@RequestBody RegisterRequest request) {
        return ApiResult.ok(auth.register(request.username(), request.password(), request.displayName()));
    }

    @PostMapping("/login")
    public ApiResult<AccountView> login(@RequestBody LoginRequest login, HttpServletRequest request,
                                       HttpServletResponse response) {
        AuthService.LoginResult result = auth.verifyLogin(login.username(), login.password());
        AuthPrincipal principal = result.principal();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));

        loginSessionStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        return ApiResult.ok(result.user());
    }

    @GetMapping("/me")
    public ApiResult<AccountView> me() {
        return ApiResult.ok(auth.currentAccount());
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return ApiResult.ok();
    }
}
