package org.example.backend.auth;

import org.example.backend.common.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserAccountController {

    public record StatusRequest(String status) {
    }

    private final AuthService auth;

    public UserAccountController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping
    public ApiResult<List<AccountView>> list(@RequestParam(name = "role", required = false) String role,
                                             @RequestParam(name = "status", required = false) String status) {
        return ApiResult.ok(auth.list(role, status));
    }

    @PatchMapping("/{id}/status")
    public ApiResult<AccountView> status(@PathVariable("id") Long id, @RequestBody StatusRequest request) {
        return ApiResult.ok(auth.changeStatus(id, request.status()));
    }
}
