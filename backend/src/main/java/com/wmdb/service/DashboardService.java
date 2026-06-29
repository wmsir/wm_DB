package com.wmdb.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wmdb.mapper.SqlTicketMapper;
import com.wmdb.model.SqlTicket;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Dashboard 服务
 * <p>
 * 聚合统计各种数据库指标。
 * </p>
 *
 * @author wm
 * @date 2023-10-25
 */
@Service
public class DashboardService {

    private final SqlTicketMapper sqlTicketMapper;

    public DashboardService(SqlTicketMapper sqlTicketMapper) {
        this.sqlTicketMapper = sqlTicketMapper;
    }

    /**
     * 获取汇总统计数据
     */
    public Map<String, Object> getDashboardStats() {
        long totalTickets = sqlTicketMapper.selectCount(new QueryWrapper<>());
        long pendingTickets = sqlTicketMapper.selectCount(new QueryWrapper<SqlTicket>().eq("status", "AUDITING"));
        long riskTickets = sqlTicketMapper.selectCount(new QueryWrapper<SqlTicket>().eq("risk_level", "HIGH"));

        Map<String, Object> stats = new HashMap<>();
        stats.put("healthScore", 98); // Still mocked for now, needs DB instance scanning
        stats.put("totalSqls", 12500); // Need AST log scanning
        stats.put("riskSqls", riskTickets);
        stats.put("totalTickets", totalTickets);
        stats.put("pendingTickets", pendingTickets);
        stats.put("dbaWorkload", 85); // 85%
        stats.put("approvalEfficiency", "1.5h");

        return stats;
    }

    /**
     * 获取数据库监控指标数据（模拟数据，后续需接入真实的监控平台如 Prometheus）
     */
    public Map<String, Object> getDatabaseMonitorStats() {
        Map<String, Object> monitorStats = new HashMap<>();

        // 核心性能指标
        monitorStats.put("cpuUsage", 45); // CPU使用率 (%)
        monitorStats.put("connections", 128); // 当前连接数
        monitorStats.put("slowSql", 12); // 慢SQL数量
        monitorStats.put("tps", 850); // 每秒事务数
        monitorStats.put("qps", 3200); // 每秒查询数
        monitorStats.put("lockWaits", 3); // 锁等待次数
        monitorStats.put("replDelay", "12ms"); // 复制延迟

        // 存储指标
        monitorStats.put("diskSpaceUsage", 68); // 磁盘空间使用率 (%)
        monitorStats.put("tableSpaceUsage", 55); // 表空间使用率 (%)
        monitorStats.put("bufferPoolHitRate", 99.2); // Buffer Pool 命中率 (%)

        return monitorStats;
    }
}
