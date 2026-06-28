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
}
