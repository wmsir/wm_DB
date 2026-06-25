package com.wmdb.controller;

import com.wmdb.service.AuthService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * <p>
 * 提供实名制登录 API 接口，返回验证通过后的 JWT 令牌。
 * </p>
 *
 * @author Jules
 * @date 2023-10-25
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 构造函数注入 AuthService
     *
     * @param authService 认证服务类
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录接口
     *
     * @param loginRequest 登录请求参数封装
     * @return 包含 Token 的响应实体
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            String token = authService.login(loginRequest.getIdCard(), loginRequest.getPassword());
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * 登录请求参数封装类
     */
    @Data
    public static class LoginRequest {
        /**
         * 身份证号码
         */
        private String idCard;

        /**
         * 密码
         */
        private String password;
    }
}
