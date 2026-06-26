package com.wmdb.controller;

import com.wmdb.service.AuthService;
import lombok.Data;
import com.wmdb.common.Result;
import com.wmdb.common.annotation.RateLimit;
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
 * @author wm
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
    @RateLimit(time = 60, count = 5) // Prevent brute-force: max 5 requests per minute per IP
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
        String token = authService.login(loginRequest.getIdCard(), loginRequest.getPassword());
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return Result.success(response);
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
