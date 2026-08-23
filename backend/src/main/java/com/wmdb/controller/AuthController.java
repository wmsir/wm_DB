package com.wmdb.controller;

import com.wmdb.common.Result;
import com.wmdb.common.annotation.RateLimit;
import com.wmdb.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * <p>
 * 提供账号密码登录、手机验证码登录及短信验证码发送 API 接口。
 * </p>
 *
 * @author wm
 * @date 2023-10-25
 */
@Tag(name = "用户认证与授权", description = "提供手机号验证码登录、用户名/身份证/手机号密码登录及短信发送")
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
     * 发送手机短信验证码
     *
     * @param sendCodeRequest 手机号封装
     * @return 包含发送状态与测试验证码的响应实体
     */
    @Operation(summary = "发送手机短信验证码", description = "输入11位手机号获取短信验证码（测试通用码：123456）")
    @RateLimit(time = 60, count = 10) // 1分钟内限制发送10次
    @PostMapping("/send-code")
    public Result<Map<String, String>> sendCode(@RequestBody SendCodeRequest sendCodeRequest) {
        String code = authService.sendSmsCode(sendCodeRequest.getPhone());
        Map<String, String> response = new HashMap<>();
        response.put("phone", sendCodeRequest.getPhone());
        response.put("code", code); // 开发/测试环境返回，便于调试
        response.put("message", "验证码发送成功");
        return Result.success(response);
    }

    /**
     * 用户统一登录接口（支持 账号密码登录 与 手机验证码登录）
     *
     * @param loginRequest 登录请求参数封装
     * @return 包含 Token 的响应实体
     */
    @Operation(summary = "用户登录认证", description = "支持账号密码登录（用户名/身份证号/手机号）与手机验证码快速登录")
    @RateLimit(time = 60, count = 10)
    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
        String token;

        // 判断登录类型：手机验证码登录
        if ("PHONE_CODE".equalsIgnoreCase(loginRequest.getLoginType()) ||
                (loginRequest.getPhone() != null && !loginRequest.getPhone().isEmpty() && loginRequest.getCode() != null && !loginRequest.getCode().isEmpty())) {
            token = authService.loginByPhone(loginRequest.getPhone(), loginRequest.getCode());
        } else {
            // 账号密码登录（兼容 account, idCard, username 传参）
            String account = loginRequest.getAccount();
            if (account == null || account.isEmpty()) {
                account = loginRequest.getIdCard();
            }
            if (account == null || account.isEmpty()) {
                account = loginRequest.getUsername();
            }
            if (account == null || account.isEmpty()) {
                account = loginRequest.getPhone();
            }
            token = authService.login(account, loginRequest.getPassword());
        }

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return Result.success(response);
    }

    /**
     * 多渠道用户自主注册
     */
    @Operation(summary = "用户多渠道注册", description = "支持手机号、邮箱、用户名注册，强制录入姓名，支持指定资源组")
    @RateLimit(time = 60, count = 10)
    @PostMapping("/register")
    public Result<Map<String, String>> register(@RequestBody com.wmdb.model.RegisterRequestDTO registerRequest) {
        String token = authService.register(registerRequest);
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("message", "注册成功并已自动登录");
        return Result.success(response);
    }

    /**
     * 获取当前登录用户画像、角色与已授权的系统页签功能权限列表
     */
    @Operation(summary = "获取当前登录用户信息及页签权限", description = "解析 JWT Token 并返回用户名、角色、组织及可访问的页签路由清单")
    @GetMapping("/user-info")
    public Result<Map<String, Object>> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return Result.success(authService.getCurrentUserInfo(authHeader));
    }

    /**
     * 发送短信验证码请求封装类
     */
    @Data
    public static class SendCodeRequest {
        private String phone;
    }

    /**
     * 登录请求参数封装类
     */
    @Data
    public static class LoginRequest {
        /**
         * 登录模式：ACCOUNT_PASSWORD (默认) 或 PHONE_CODE
         */
        private String loginType;

        /**
         * 统一登录账号（用户名、身份证或手机号）
         */
        private String account;

        /**
         * 身份证号码（兼容旧接口）
         */
        private String idCard;

        /**
         * 用户名（兼容传参）
         */
        private String username;

        /**
         * 手机号
         */
        private String phone;

        /**
         * 密码（SM2 加密或明文）
         */
        private String password;

        /**
         * 短信验证码
         */
        private String code;
    }
}
