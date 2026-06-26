package com.wmdb.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 数据库实例实体类
 * <p>
 * 映射 db_instance 表，存储纳管的目标数据库实例连接信息及安全凭证。
 * </p>
 *
 * @author Jules
 * @date 2023-10-25
 */
@Data
@TableName("db_instance")
public class DbInstance {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 实例名称
     */
    private String name;

    /**
     * 数据库类型（如 mysql, dameng）
     */
    private String dbType;

    /**
     * JDBC 连接串
     */
    private String jdbcUrl;

    /**
     * 只读从库 JDBC 连接串（可选，用于 DQL 读写分离）
     */
    private String readOnlyJdbcUrl;

    /**
     * 数据库账号
     */
    private String username;

    /**
     * 密码（AES 加密存储）
     */
    private String passwordCipher;

    /**
     * 所属环境（DEV, TEST, PROD）
     */
    private String env;
}
