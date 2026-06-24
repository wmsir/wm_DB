package com.wmdb.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("db_instance")
public class DbInstance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String dbType;
    private String jdbcUrl;
    private String username;
    private String passwordCipher;
    private String env;
}
