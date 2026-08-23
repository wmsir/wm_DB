package com.wmdb.runner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wmdb.mapper.*;
import com.wmdb.model.*;
import com.wmdb.security.SmUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 全系统真实业务测试数据与多角色测试账号初始化器
 * <p>
 * 保证系统启动时具备完整的真实业务演练数据：
 * 1. 各审批节点测试账号（超级管理员、开发组长初审、核心DBA运维、安全合规审查、普通开发申请人）；
 * 2. 覆盖各业务资源组、各状态生命周期（待初审、待复核、待执行反馈、已执行归档、已驳回）的真实 SQL 工单与回滚方案；
 * 3. 真实执行审计与耗时日志记录。
 * </p>
 *
 * @author wm
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class TestDataInitializerRunner implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final SqlTicketMapper sqlTicketMapper;
    private final SqlTicketDetailMapper sqlTicketDetailMapper;
    private final SqlAuditLogMapper sqlAuditLogMapper;
    private final DbInstanceMapper dbInstanceMapper;

    // 默认标准密码 123456 的 BCrypt 哈希
    private static final String DEFAULT_PWD_HASH = "$2a$10$f1pqjyVOHcJWgUGQCmz.B.QXefiemDBivlwLMAZpBNppJdsfM4RuW";

    @Override
    public void run(String... args) {
        log.info("====== 开始初始化全系统真实业务测试数据与审批节点角色账号 ======");
        initTestAccounts();
        initRealisticTickets();
        log.info("====== 全系统真实业务测试数据与角色账号初始化完成 ======");
    }

    /**
     * 初始化各审批节点配套的专属测试账号 (统一密码 123456，支持多业务资源组归属)
     */
    private void initTestAccounts() {
        // 1. 超级管理员 (所有节点全权审批，归属全平台资源组)
        ensureUser("testadmin1", "王总", "ADMIN", "全平台最高决策组, 车险承保资源组, 销管系统资源组, 理赔服务核心组, 默认核心业务资源组", "13800000001", "310101198501010001", "admin1@wmdb.com");
        ensureUser("admin", "超级管理员", "ADMIN", "全平台最高决策组, 默认核心业务资源组", "13800000000", "0", "admin@wmdb.com");

        // 2. 开发组长初审节点测试账号 (可兼管多个业务线)
        ensureUser("testadmin2", "张伟", "DEV_LEAD", "车险承保资源组, 理赔服务核心组", "13800000002", "310101198802020002", "zhangwei_lead@wmdb.com");
        ensureUser("leader_sales", "陈敏", "DEV_LEAD", "销管系统资源组, 水险财产险1000条以下", "13800000012", "310101198805050012", "chenmin_lead@wmdb.com");

        // 3. 核心 DBA 安全复核与线下执行节点测试账号
        ensureUser("testadmin3", "赵工", "DBA", "生产DBA运维保障组, 默认核心业务资源组", "13800000003", "310101199003030003", "zhaodba@wmdb.com");
        ensureUser("dba_master", "钱工", "DBA", "生产DBA运维保障组, 农险理赔资源组", "13800000013", "310101199006060013", "qiandba@wmdb.com");

        // 4. 数据安全与合规审查节点测试账号
        ensureUser("test_auditor", "李安全", "AUDITOR", "全平台合规审计组, 风勘中心资源组", "13800000004", "310101199204040004", "lisecurity@wmdb.com");

        // 5. 普通开发人员 (工单发起与查询，支持跨项目组)
        ensureUser("test_dev", "张三", "DEV", "车险承保资源组, 销管系统资源组", "13800000005", "310101199505050005", "zhangsan_dev@wmdb.com");
        ensureUser("sales_dev", "李四", "DEV", "销管系统资源组", "13800000006", "310101199606060006", "lisi_dev@wmdb.com");
        ensureUser("claims_dev", "刘五", "DEV", "理赔服务核心组, 互联网车主服务与理赔快处组", "13800000007", "310101199707070007", "liuwu_dev@wmdb.com");
    }

    private void ensureUser(String username, String realName, String role, String resourceGroup, String phone, String idCard, String email) {
        SysUser exist = sysUserMapper.selectOne(new QueryWrapper<SysUser>().eq("username", username));
        if (exist == null) {
            SysUser user = new SysUser();
            user.setTenantId("1");
            user.setUsername(username);
            user.setRealName(realName);
            user.setRole(role);
            user.setResourceGroup(resourceGroup);
            user.setPhone(phone);
            user.setIdCard(idCard);
            user.setEmail(email);
            user.setPasswordHash(DEFAULT_PWD_HASH);
            user.setPasswordCipher(SmUtils.sm3Hash("123456"));
            user.setStatus(1);
            sysUserMapper.insert(user);
            log.info("Initialized test user: {} ({}) with role {}", username, realName, role);
        } else {
            exist.setRealName(realName);
            exist.setRole(role);
            exist.setResourceGroup(resourceGroup);
            exist.setPhone(phone);
            exist.setIdCard(idCard);
            exist.setEmail(email);
            exist.setPasswordHash(DEFAULT_PWD_HASH);
            exist.setPasswordCipher(SmUtils.sm3Hash("123456"));
            sysUserMapper.updateById(exist);
        }
    }

    /**
     * 初始化具有真实业务场景、涵盖各审批流生命周期的仿真工单数据
     */
    private void initRealisticTickets() {
        Long inst1 = 1L; // 阿里云RDS-车险与销管核心生产库
        Long count = sqlTicketMapper.selectCount(new QueryWrapper<>());
        if (count != null && count >= 8) {
            log.info("系统已有 {} 条工单数据，跳过测试工单注入", count);
            return;
        }

        // ==================== 工单 1：待开发组长初审 (DML 变更) ====================
        createTicketIfNotExist(
                1788001001001L,
                "310101199505050005", // 张三 (车险开发)
                inst1,
                "SQL_AUDIT",
                "AUDITING",
                "[目标库: huiqitong_erp] 2026续保核心配置上线 (复制续保清空配置)",
                "-- 1\nINSERT INTO typ_preference (PREFERENCEID, CREATEDATE, UPDATEDATE, DESCRIPTION, actortype, kind, name, value, profile)\nSELECT COALESCE(MAX(PREFERENCEID), 0) + 1, NOW(), NOW(), '复制续保清空配置-18080003分片1', 'System', 'copyClearFieldsConfig', 'copyClearFieldsConfig', 'plcWarrLineSupply:netPlatSendBackFlag=0', '18080003'\nFROM typ_preference\nWHERE PREFERENCEID >= 1 AND PREFERENCEID <= 99999999;",
                "-- 1\n-- 数据回滚补偿语句：恢复 typ_preference 表配置数据\nDELETE FROM typ_preference WHERE name = 'copyClearFieldsConfig' AND profile = '18080003';",
                1,
                null
        );

        // ==================== 工单 2：待 DBA 安全复核与执行 (DDL 提速索引) ====================
        createTicketIfNotExist(
                1788001002002L,
                "310101199606060006", // 李四 (销管开发)
                inst1,
                "SQL_AUDIT",
                "AUDITING",
                "[目标库: huiqitong_erp] 渠道佣金结算查询性能调优 (创建复合加速索引)",
                "-- 0\nCREATE INDEX idx_pref_name_prof ON typ_preference (name, profile, UPDATEDATE);",
                "-- 0\n-- 索引回滚方案：删除新建索引\nDROP INDEX idx_pref_name_prof ON typ_preference;",
                0,
                null
        );

        // ==================== 工单 3：已转由 DBA 专用工具线下执行 (MANUAL_PROCESSING, 待DBA反馈归档) ====================
        createTicketIfNotExist(
                1788001003003L,
                "310101199505050005", // 张三
                inst1,
                "SQL_AUDIT",
                "MANUAL_PROCESSING",
                "[目标库: huiqitong_erp] 历史承保保单大体量数据分批归档与物理表空间优化",
                "-- 100\nUPDATE typ_preference SET DESCRIPTION = '归档优化-完成' WHERE PREFERENCEID BETWEEN 1000 AND 1099;",
                "-- 100\nUPDATE typ_preference SET DESCRIPTION = '历史默认配置' WHERE PREFERENCEID BETWEEN 1000 AND 1099;",
                100,
                "转由 DBA 工具手工线下执行并反馈结果"
        );

        // ==================== 工单 4：已自动化流式执行完毕 (EXECUTED, 归档并记录审计日志) ====================
        SqlTicket t4 = createTicketIfNotExist(
                1788001004004L,
                "310101199707070007", // 刘五 (理赔核心)
                inst1,
                "SQL_AUDIT",
                "EXECUTED",
                "[目标库: huiqitong_erp] 理赔反欺诈黑名单与高风险客户配置字典更新",
                "-- 1\nUPDATE typ_preference SET value = 'antiFraudLevel=HIGH,autoAudit=0' WHERE name = 'copyClearFieldsConfig' AND profile = '18080003';",
                "-- 1\nUPDATE typ_preference SET value = 'antiFraudLevel=NORMAL,autoAudit=1' WHERE name = 'copyClearFieldsConfig' AND profile = '18080003';",
                1,
                "立即自动化流式执行成功 (耗时: 38ms)"
        );
        if (t4 != null) {
            SqlAuditLog log4 = new SqlAuditLog();
            log4.setTicketId(t4.getId());
            log4.setExecuteSql("UPDATE typ_preference SET value = 'antiFraudLevel=HIGH,autoAudit=0' WHERE name = 'copyClearFieldsConfig' AND profile = '18080003';");
            log4.setCostTimeMs(38L);
            log4.setStatus("SUCCESS");
            sqlAuditLogMapper.insert(log4);
        }

        // ==================== 工单 5：合规与敏感数据导出申请 (待合规专员审查) ====================
        createTicketIfNotExist(
                1788001005005L,
                "310101199606060006", // 李四
                inst1,
                "DATA_EXPORT",
                "AUDITING",
                "[目标库: huiqitong_erp] 2026 年中全国渠道代理人佣金结算脱敏明细数据导出",
                "SELECT profile, name, value, UPDATEDATE FROM typ_preference WHERE name LIKE '%Config%' LIMIT 500;",
                "-- 数据导出工单无回滚 SQL 脚本",
                500,
                null
        );

        // ==================== 工单 6：已驳回工单 (REJECTED, 存在高危全表更新风险) ====================
        createTicketIfNotExist(
                1788001006006L,
                "310101199707070007", // 刘五
                inst1,
                "SQL_AUDIT",
                "REJECTED",
                "[目标库: huiqitong_erp] 批量更新系统全表参数状态 (已驳回)",
                "-- 0\nUPDATE typ_preference SET DESCRIPTION = '全表危险更新测试';",
                "-- 0\n-- 无回滚",
                0,
                "[驳回原因: 严禁提交无 WHERE 范围条件的全表更新语句，存在重大生产事故风险！]"
        );
    }

    private SqlTicket createTicketIfNotExist(Long ticketId, String applicantIdCard, Long instanceId, String type,
                                             String status, String reason, String sqlText, String rollbackSql,
                                             Integer affectRows, String executionWindow) {
        SqlTicket exist = sqlTicketMapper.selectById(ticketId);
        if (exist != null) {
            return exist;
        }

        SqlTicket ticket = new SqlTicket();
        ticket.setId(ticketId);
        ticket.setBusinessKey(UUID.randomUUID().toString());
        ticket.setApplicantIdCard(applicantIdCard);
        ticket.setInstanceId(instanceId);
        ticket.setType(type);
        ticket.setStatus(status);
        ticket.setReason(reason);
        ticket.setExecutionWindow(executionWindow);
        sqlTicketMapper.insert(ticket);

        SqlTicketDetail detail = new SqlTicketDetail();
        detail.setId(ticketId + 1);
        detail.setTicketId(ticketId);
        detail.setSqlText(sqlText);
        detail.setRollbackSqlText(rollbackSql);
        detail.setAffectRowsEstimate(affectRows != null ? affectRows : 1);
        sqlTicketDetailMapper.insert(detail);

        log.info("Initialized realistic ticket #{}: {}", ticketId, reason);
        return ticket;
    }
}