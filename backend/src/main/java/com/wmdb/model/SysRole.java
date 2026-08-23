package com.wmdb.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色实体类
 * <p>
 * 映射 sys_role 表。
 * </p>
 *
 * @author wm
 * @date 2023-10-25
 */
@Data
@TableName("sys_role")
public class SysRole {

    private String tenantId;

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 允许访问的系统页签与功能权限列表（JSON 字符串格式，存储路由路径列表如 ["/dashboard", "/ticket-list", ...]）
     */
    private String permissions;
}
