package com.wmdb.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sql_audit_log")
public class SqlAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private String executeSql;
    private Long costTimeMs;
    private String status;
    private String errorTrace;
    private String previousHash;
    private String currentHash;
}
