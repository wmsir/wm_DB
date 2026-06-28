package com.wmdb.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 系统用户实体类
 * <p>
 * 映射 sys_user 表，存储用户真实姓名、身份证（登录名）、角色等实名制安全信息。
 * </p>
 *
 * @author wm
 * @date 2023-10-25
 */
@Data
@TableName("sys_user")
public class SysUser {

    private String tenantId;

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 身份证号码（系统唯一登录名）
     */
    private String idCard;

    /**
     * 加密存储的密码
     */
    private String passwordCipher;

    /**
     * 角色标识（如：ADMIN, DBA, DEV）
     */
    private String role;
}
