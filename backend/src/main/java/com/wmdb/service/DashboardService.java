package com.wmdb.service;

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

    /**
     * 获取汇总统计数据 (Mock)
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("healthScore", 98);
        stats.put("totalSqls", 12500);
        stats.put("riskSqls", 34);
        stats.put("totalTickets", 156);
        stats.put("pendingTickets", 5);
        stats.put("dbaWorkload", 85); // 85%
        stats.put("approvalEfficiency", "1.5h");

        return stats;
    }
}
