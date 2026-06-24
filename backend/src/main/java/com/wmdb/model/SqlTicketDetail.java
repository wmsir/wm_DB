package com.wmdb.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sql_ticket_detail")
public class SqlTicketDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private String sqlText;
    private String attachmentOssKey;
    private Integer affectRowsEstimate;
}
