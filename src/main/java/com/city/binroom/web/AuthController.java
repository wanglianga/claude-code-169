package com.city.binroom.web;

import com.city.binroom.web.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "鉴权")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "工作人员登录（物业/环卫/街道/管理员）")
    @PostMapping("/staff/login")
    public Map<String, Object> staffLogin(@RequestBody StaffLoginRequest req) {
        return authService.staffLogin(req.getUsername(), req.getPassword());
    }

    @Operation(summary = "居民刷卡/扫码登录（输入卡号）")
    @PostMapping("/resident/login")
    public Map<String, Object> residentLogin(@RequestBody ResidentLoginRequest req) {
        return authService.residentLogin(req.getCardNo());
    }

    @Data
    public static class StaffLoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Data
    public static class ResidentLoginRequest {
        @NotBlank
        private String cardNo;
    }
}
