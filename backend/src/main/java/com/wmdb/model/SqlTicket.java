package com.wmdb.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sql_ticket")
public class SqlTicket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String businessKey;
    private Long instanceId;
    private String applicantIdCard;
    private String status;
    private String riskLevel;
    private String flowInstanceId;
}
