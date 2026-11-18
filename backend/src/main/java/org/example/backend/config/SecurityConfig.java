package org.example.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.backend.auth.AccountSessionFilter;
import org.example.backend.auth.AuthFailureWriter;
import org.example.backend.auth.UserAccountMapper;
import org.example.backend.common.ErrorCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;
import java.util.function.Supplier;

@Configuration
public class SecurityConfig {

    private static final RequestMatcher PUBLIC_ENDPOINTS = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/auth/csrf", "GET"),
            new AntPathRequestMatcher("/api/auth/register", "POST"),
            new AntPathRequestMatcher("/api/auth/login", "POST"));

    @Bean
    SecurityFilterChain parkingSecurity(HttpSecurity http, SecurityContextRepository contexts,
                                        CsrfTokenRepository tokens, UserAccountMapper accounts,
                                        SecurityContextLogoutHandler logoutHandler, ObjectMapper objectMapper)
            throws Exception {
        AuthFailureWriter failures = new AuthFailureWriter(objectMapper);
        return http
                .securityContext(context -> context.securityContextRepository(contexts).requireExplicitSave(true))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .csrf(csrf -> csrf.csrfTokenRepository(tokens).csrfTokenRequestHandler(csrfHeaderHandler()))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) ->
                                failures.write(response, ErrorCode.UNAUTHENTICATED, "请先登录"))
                        .accessDeniedHandler((request, response, error) -> {
                            if (error instanceof CsrfException) {
                                failures.write(response, ErrorCode.INVALID_CSRF, "CSRF 令牌缺失或无效");
                            } else {
                                failures.write(response, ErrorCode.FORBIDDEN, "权限不足");
                            }
                        }))
                .addFilterAfter(new AccountSessionFilter(PUBLIC_ENDPOINTS, accounts, logoutHandler, failures),
                        SecurityContextHolderFilter.class)
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers("/api/owner/**").hasRole("OWNER")
                        .requestMatchers("/api/users/**", "/api/parking-lots/**", "/api/parking-areas/**",
                                "/api/parking-spaces/**", "/api/fee-rules/**", "/api/parking-entries/**",
                                "/api/parking-exits/**", "/api/parking-records/**", "/api/dashboard/**")
                        .hasRole("ADMIN")
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
        contexts.setDisableUrlRewriting(true);
        return contexts;
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository tokens = new HttpSessionCsrfTokenRepository();
        tokens.setHeaderName("X-XSRF-TOKEN");
        return tokens;
    }

    @Bean
    SessionAuthenticationStrategy loginSessionStrategy(CsrfTokenRepository tokens) {
        CsrfAuthenticationStrategy refreshCsrf = new CsrfAuthenticationStrategy(tokens);
        refreshCsrf.setRequestHandler(csrfHeaderHandler());
        return new CompositeSessionAuthenticationStrategy(
                List.of(new ChangeSessionIdAuthenticationStrategy(), refreshCsrf));
    }

    @Bean
    SecurityContextLogoutHandler sessionLogoutHandler(SecurityContextRepository contexts) {
        SecurityContextLogoutHandler handler = new SecurityContextLogoutHandler();
        handler.setSecurityContextRepository(contexts);
        return handler;
    }

    // 对外返回 Spring 的掩码令牌；写请求只接受约定请求头，不接受参数中的令牌。
    private static CsrfTokenRequestHandler csrfHeaderHandler() {
        XorCsrfTokenRequestAttributeHandler delegate = new XorCsrfTokenRequestAttributeHandler();
        return new CsrfTokenRequestHandler() {
            @Override
            public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> token) {
                delegate.handle(request, response, token);
            }

            @Override
            public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
                return request.getHeader(token.getHeaderName()) == null
                        ? null : delegate.resolveCsrfTokenValue(request, token);
            }
        };
    }
}
