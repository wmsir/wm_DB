package com.wmdb.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wmdb.mapper.SysUserMapper;
import com.wmdb.model.SysUser;
import com.wmdb.security.JwtUtils;
import com.wmdb.security.RsaUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 认证授权服务
 * <p>
 * 处理基于身份证号码的实名登录逻辑，校验凭据并签发 JWT Token。
 * </p>
 *
 * @author Jules
 * @date 2023-10-25
 */
@Service
public class AuthService {

    private final JwtUtils jwtUtils;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final RsaUtils rsaUtils;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 构造函数注入依赖
     *
     * @param jwtUtils JWT 工具类
     * @param sysUserMapper 用户 Mapper
     * @param passwordEncoder 密码加密器
     * @param rsaUtils RSA 解密工具
     * @param redisTemplate Redis 缓存模板
     */
    public AuthService(JwtUtils jwtUtils, SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder,
                       RsaUtils rsaUtils, RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtils = jwtUtils;
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.rsaUtils = rsaUtils;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 执行登录逻辑
     *
     * @param idCard 身份证号码
     * @param password 密码
     * @return 登录成功后签发的 JWT Token
     */
    public String login(String idCard, String encryptedPassword) {
        try {
            // Decrypt password
            String password = rsaUtils.decrypt(encryptedPassword);

            // Find user by ID card
            SysUser user = sysUserMapper.selectOne(new QueryWrapper<SysUser>().eq("id_card", idCard));

            // Strictly verify credentials against DB using BCrypt
            if (user == null || !passwordEncoder.matches(password, user.getPasswordCipher())) {
                throw new RuntimeException("Invalid credentials");
            }

            String token = jwtUtils.generateToken(user.getIdCard(), user.getRealName());

            // Track active login in Redis
            try {
                redisTemplate.opsForValue().set("login_token:" + idCard, token, 24, TimeUnit.HOURS);
            } catch (Exception e) {
                System.err.println("Redis connection failed, skipping cache: " + e.getMessage());
            }

            return token;
        } catch (Exception e) {
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }
}
