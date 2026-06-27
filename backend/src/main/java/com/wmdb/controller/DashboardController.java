package com.wmdb.controller;

import com.wmdb.common.Result;
import com.wmdb.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Dashboard 控制器
 * <p>
 * 提供首页大盘指标统计数据。
 * </p>
 *
 * @author wm
 * @date 2023-10-25
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 获取大盘统计数据
     *
     * @return 统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(dashboardService.getDashboardStats());
    }
}
