package com.wmdb.controller;

import com.wmdb.common.Result;
import com.wmdb.model.SysRole;
import com.wmdb.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 *
 * @author wm
 * @date 2023-10-25
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @GetMapping("/list")
    public Result<List<SysRole>> listRoles() {
        return Result.success(sysRoleService.listRoles());
    }

    @PostMapping("/save")
    public Result<Void> saveRole(@RequestBody SysRole role) {
        sysRoleService.saveRole(role);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable("id") Long id) {
        sysRoleService.deleteRole(id);
        return Result.success(null);
    }
}
