package com.wmdb.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wmdb.mapper.SysRoleMapper;
import com.wmdb.model.SysRole;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色与页签功能权限管理服务
 *
 * @author wm
 * @date 2023-10-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final com.wmdb.mapper.SysUserMapper sysUserMapper;
    private final UserDisplayNameService userDisplayNameService;
    private final JdbcTemplate jdbcTemplate;

    public static final String PERM_ALL = "[\"*\"]";

    public static final String PERM_DBA = "[\"/dashboard\",\"/ticket-list\",\"/ticket-create\",\"/ai-sql-review\",\"/data-query\",\"/data-masking\",\"/audit-dashboard\",\"/instance-list\",\"/instance-sessions\",\"/instance-databases\",\"/instance-accounts\",\"/instance-params\",\"/resource-group-list\",\"/workflow-designer\"]";

    public static final String PERM_DEV_LEAD = "[\"/dashboard\",\"/ticket-list\",\"/ticket-create\",\"/ai-sql-review\",\"/data-query\",\"/data-masking\",\"/audit-dashboard\",\"/resource-group-list\",\"/workflow-designer\"]";

    public static final String PERM_DEV = "[\"/dashboard\",\"/ticket-list\",\"/ticket-create\",\"/ai-sql-review\",\"/data-query\",\"/audit-dashboard\"]";

    public static final String PERM_AUDITOR = "[\"/dashboard\",\"/ticket-list\",\"/ai-sql-review\",\"/data-masking\",\"/audit-dashboard\",\"/license\",\"/settings\"]";

    /**
     * 系统启动初始化默认预置角色及页签权限
     */
    @PostConstruct
    public void initDefaultRoles() {
        try {
            // 确保 permissions 字段存在
            try {
                jdbcTemplate.execute("ALTER TABLE sys_role ADD COLUMN permissions TEXT");
                log.info("Added permissions column to sys_role table successfully.");
            } catch (Exception ignored) {
                // Column already exists
            }

            insertOrUpdateRole("ADMIN", "超级管理员", "拥有平台最高特权，具备全量功能页签、系统配置、租户管理与全权审批", PERM_ALL);
            insertOrUpdateRole("DBA", "核心数据库管理员", "负责数据库实例纳管、会话与账号管理、高危工单终审与 BPMN 流程编排", PERM_DBA);
            insertOrUpdateRole("DEV_LEAD", "业务开发组长", "负责业务工单初审、DML 审核、资源组分配与团队日常变更管控", PERM_DEV_LEAD);
            insertOrUpdateRole("DEV", "研发工程师", "拥有 SQL 工单提交、事务预执行校验、AI 智能审核与数据查询控制台权限", PERM_DEV);
            insertOrUpdateRole("AUDITOR", "安全合规审计员", "负责安全大盘监控、数据动态脱敏配置、审计日志核查与合规报表查看", PERM_AUDITOR);
            insertOrUpdateRole("OPS", "业务系统运维", "负责系统日常运维、实例监控调度与基础设施巡检", PERM_DEV_LEAD);
            insertOrUpdateRole("SECURITY", "数据安全官", "负责全平台敏感数据资产保护、动态脱敏算法与最高安全合规管控", PERM_AUDITOR);

            log.info("Initialized system roles (7 roles) and default tab permissions successfully.");
        } catch (Exception e) {
            log.warn("初始化默认角色及页签权限异常: {}", e.getMessage());
        }
    }

    private void insertOrUpdateRole(String code, String name, String desc, String defaultPermissions) {
        SysRole existing = sysRoleMapper.selectOne(new QueryWrapper<SysRole>().eq("role_code", code));
        if (existing == null) {
            SysRole role = new SysRole();
            role.setTenantId("1");
            role.setRoleCode(code);
            role.setRoleName(name);
            role.setDescription(desc);
            role.setPermissions(defaultPermissions);
            sysRoleMapper.insert(role);
        } else if (existing.getPermissions() == null || existing.getPermissions().trim().isEmpty()) {
            existing.setPermissions(defaultPermissions);
            existing.setRoleName(name);
            existing.setDescription(desc);
            sysRoleMapper.updateById(existing);
        }
    }

    public List<SysRole> listRoles() {
        List<SysRole> list = sysRoleMapper.selectList(new QueryWrapper<>());
        if (list == null || list.isEmpty()) {
            initDefaultRoles();
            list = sysRoleMapper.selectList(new QueryWrapper<>());
        }
        enrichRoleMembers(list);
        return list;
    }

    public com.wmdb.model.PageResultDTO<SysRole> pageRoles(int page, int size, String keyword) {
        QueryWrapper<SysRole> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            qw.and(w -> w.like("role_name", kw)
                    .or().like("role_code", kw)
                    .or().like("description", kw));
        }
        qw.orderByAsc("id");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysRole> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page > 0 ? page : 1, size > 0 ? size : 10);
        sysRoleMapper.selectPage(mpPage, qw);
        enrichRoleMembers(mpPage.getRecords());
        return com.wmdb.model.PageResultDTO.from(mpPage);
    }

    private void enrichRoleMembers(List<SysRole> roles) {
        if (roles == null || roles.isEmpty()) return;
        List<com.wmdb.model.SysUser> users = sysUserMapper.selectList(new QueryWrapper<com.wmdb.model.SysUser>().eq("status", "1"));
        for (SysRole role : roles) {
            String code = role.getRoleCode();
            List<String> memberNames = new ArrayList<>();
            if (users != null) {
                for (com.wmdb.model.SysUser u : users) {
                    List<String> uRoles = userDisplayNameService.parseRoles(u.getRole());
                    boolean matched = uRoles.contains(code);
                    if (!matched && "ADMIN".equalsIgnoreCase(code)) {
                        matched = "admin".equalsIgnoreCase(u.getUsername()) || "testadmin1".equalsIgnoreCase(u.getUsername());
                    }
                    if (matched) {
                        String displayName = userDisplayNameService.getDisplayName(u);
                        String label = (u.getUsername() != null && !u.getUsername().equalsIgnoreCase(displayName))
                                ? displayName + " (" + u.getUsername() + ")"
                                : displayName;
                        if (!memberNames.contains(label)) {
                            memberNames.add(label);
                        }
                    }
                }
            }
            role.setMemberNames(memberNames);
            role.setMemberCount(memberNames.size());
        }
    }

    public void saveRole(SysRole role) {
        if (role.getTenantId() == null || role.getTenantId().isEmpty()) {
            role.setTenantId("1");
        }
        if (role.getPermissions() == null || role.getPermissions().trim().isEmpty()) {
            role.setPermissions(getDefaultPermissionsForCode(role.getRoleCode()));
        }
        if (role.getId() == null) {
            sysRoleMapper.insert(role);
        } else {
            sysRoleMapper.updateById(role);
        }
    }

    public void deleteRole(Long id) {
        sysRoleMapper.deleteById(id);
    }

    public String getDefaultPermissionsForCode(String roleCode) {
        if (roleCode == null) return PERM_DEV;
        switch (roleCode.toUpperCase()) {
            case "ADMIN":
                return PERM_ALL;
            case "DBA":
                return PERM_DBA;
            case "DEV_LEAD":
            case "LEAD":
                return PERM_DEV_LEAD;
            case "AUDITOR":
                return PERM_AUDITOR;
            case "DEV":
            default:
                return PERM_DEV;
        }
    }

    /**
     * 根据角色编码查询已授权的页签路由路径列表
     */
    public List<String> getPermissionsByRole(String roleCode) {
        if ("ADMIN".equalsIgnoreCase(roleCode)) {
            return List.of("*");
        }
        SysRole role = sysRoleMapper.selectOne(new QueryWrapper<SysRole>().eq("role_code", roleCode));
        String permJson = (role != null && role.getPermissions() != null && !role.getPermissions().trim().isEmpty())
                ? role.getPermissions()
                : getDefaultPermissionsForCode(roleCode);

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(permJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of("/dashboard", "/ticket-list", "/ticket-create", "/data-query");
        }
    }
}
