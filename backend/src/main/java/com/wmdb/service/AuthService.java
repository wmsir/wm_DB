package com.wmdb.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wmdb.mapper.SysUserMapper;
import com.wmdb.model.SysUser;
import com.wmdb.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    /**
     * 构造函数注入依赖
     *
     * @param jwtUtils JWT 工具类
     * @param sysUserMapper 用户 Mapper
     * @param passwordEncoder 密码加密器
     */
    public AuthService(JwtUtils jwtUtils, SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder) {
        this.jwtUtils = jwtUtils;
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 执行登录逻辑
     *
     * @param idCard 身份证号码
     * @param password 密码
     * @return 登录成功后签发的 JWT Token
     */
    public String login(String idCard, String password) {
        // Find user by ID card
        SysUser user = sysUserMapper.selectOne(new QueryWrapper<SysUser>().eq("id_card", idCard));

        // Strictly verify credentials against DB using BCrypt
        if (user == null || !passwordEncoder.matches(password, user.getPasswordCipher())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtUtils.generateToken(user.getIdCard(), user.getRealName());
    }
}
