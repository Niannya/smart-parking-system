package org.example.backend.config;

import org.example.backend.auth.AccountStatusFilter;
import org.example.backend.common.RestSecurityHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 安全配置（成员 1 scaffold，与 Spring Boot 3.2.5 管理的 Spring Security 6.2.x 匹配）：
 * 服务器会话 + HttpOnly 会话 Cookie；写请求要求 CSRF token；
 * 401/403 使用统一 JSON 外壳；管理端点仅 ADMIN，车主端点仅 OWNER。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 业务时间统一 Asia/Shanghai（01 契约：按上海时区划日） */
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    @Bean
    public Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RestSecurityHandlers restSecurityHandlers,
                                                   AccountStatusFilter accountStatusFilter) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/auth/csrf"))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(restSecurityHandlers)
                .accessDeniedHandler(restSecurityHandlers))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/api/owner/**").hasRole("OWNER")
                .requestMatchers("/api/users/**", "/api/parking-lots/**", "/api/parking-areas/**",
                        "/api/parking-spaces/**", "/api/fee-rules/**", "/api/parking-entries/**",
                        "/api/parking-exits/**", "/api/parking-records/**", "/api/dashboard/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterAfter(accountStatusFilter, SecurityContextHolderFilter.class)
            .logout(LogoutConfigurer::disable)
            .requestCache(RequestCacheConfigurer::disable);
        return http.build();
    }
}
