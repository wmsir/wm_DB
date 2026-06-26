package com.wmdb.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * SQL 工单主表实体类
 * <p>
 * 映射 sql_ticket 表，记录审批流工单基础信息，串联 Flowable 审批实例。
 * </p>
 *
 * @author wm
 * @date 2023-10-25
 */
@Data
@TableName("sql_ticket")
public class SqlTicket {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 业务流水号
     */
    private String businessKey;

    /**
     * 目标数据库实例 ID
     */
    private Long instanceId;

    /**
     * 申请人身份证号码
     */
    private String applicantIdCard;

    /**
     * 工单状态（DRAFT, AUDITING, APPROVED, REJECTED, EXECUTED, FAILED）
     */
    private String status;

    /**
     * 风险等级
     */
    private String riskLevel;

    /**
     * 关联的 Flowable 流程实例 ID
     */
    private String flowInstanceId;

    /**
     * 指定的维护窗口期（如：cron 表达式或特定时间范围，为空则立即执行）
     */
    private String executionWindow;
}
