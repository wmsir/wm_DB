package com.wmdb.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wmdb.model.SysUser;
import com.wmdb.security.JwtUtils;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtUtils jwtUtils;

    // Ideally, we should inject a Mapper here (e.g., SysUserMapper)
    // to verify against DB. For this demo architecture, we'll mock it if not present.
    public AuthService(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    public String login(String idCard, String password) {
        // Mock DB verification
        // SysUser user = sysUserMapper.selectOne(new QueryWrapper<SysUser>().eq("id_card", idCard));
        // if (user != null && password.equals(user.getPasswordCipher())) { // using plain equals here, in reality should use PasswordEncoder
        //     return jwtUtils.generateToken(user.getIdCard(), user.getRealName());
        // }

        // Let's assume verification passes for demo if both are non-empty
        if (idCard != null && !idCard.isEmpty() && password != null && !password.isEmpty()) {
            return jwtUtils.generateToken(idCard, "Mock Real Name");
        }

        throw new RuntimeException("Invalid credentials");
    }
}
