package org.example.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.common.ApiResult;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 账号与会话（01 契约第 2 节）。成员 1 scaffold：只实现成员 3 链路所需最小集合。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 前端先取得 token，写请求放 X-XSRF-TOKEN */
    @GetMapping("/csrf")
    public ApiResult<Map<String, String>> csrf(CsrfToken token) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("token", token.getToken());
        data.put("headerName", token.getHeaderName());
        return ApiResult.ok(data);
    }

    @PostMapping("/register")
    public ApiResult<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResult<UserDto> login(@Valid @RequestBody LoginRequest request,
                                    HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return ApiResult.ok(authService.login(request, httpRequest, httpResponse));
    }

    @GetMapping("/me")
    public ApiResult<UserDto> me() {
        return ApiResult.ok(authService.me());
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout(httpRequest, httpResponse);
        return ApiResult.ok();
    }
}
