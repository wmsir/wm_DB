package com.wmdb.controller;

import com.wmdb.common.Result;
import com.wmdb.service.AiService;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 控制器
 * <p>
 * 对外暴露 AI 数据库治理相关接口。
 * </p>
 *
 * @author wm
 * @date 2023-10-25
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Text2SQL
     */
    @PostMapping("/text2sql")
    public Result<Map<String, Object>> textToSql(@RequestBody Text2SqlRequest request) {
        return Result.success(aiService.textToSql(request.getPrompt(), request.getDbType()));
    }

    /**
     * SQL 智能优化建议
     */
    @PostMapping("/optimize")
    public Result<String> optimizeSql(@RequestBody OptimizeRequest request) {
        return Result.success(aiService.optimizeSql(request.getSql()));
    }

    @Data
    public static class Text2SqlRequest {
        private String prompt;
        private String dbType;
    }

    @Data
    public static class OptimizeRequest {
        private String sql;
    }
}
