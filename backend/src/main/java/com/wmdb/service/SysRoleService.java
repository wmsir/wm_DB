package com.wmdb.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wmdb.mapper.SysRoleMapper;
import com.wmdb.model.SysRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色管理服务
 *
 * @author wm
 * @date 2023-10-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper sysRoleMapper;

    public List<SysRole> listRoles() {
        return sysRoleMapper.selectList(new QueryWrapper<>());
    }

    public void saveRole(SysRole role) {
        if (role.getId() == null) {
            sysRoleMapper.insert(role);
        } else {
            sysRoleMapper.updateById(role);
        }
    }

    public void deleteRole(Long id) {
        sysRoleMapper.deleteById(id);
    }
}
