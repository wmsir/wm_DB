package com.wmdb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 智能治理服务
 * <p>
 * 提供 Text2SQL、自动巡检、异常预测等核心 AI Agent 功能。
 * 底层对接支持 OpenAI 兼容格式的大模型接口（如 DeepSeek / Qwen）。
 * </p>
 *
 * @author wm
 */
@Service
public class AiService {

    @Value("${wmdb.ai.api-key:mock-api-key}")
    private String apiKey;

    @Value("${wmdb.ai.endpoint:https://api.openai.com/v1/chat/completions}")
    private String apiEndpoint;

    @Value("${wmdb.ai.model:gpt-3.5-turbo}")
    private String modelName;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 基础大模型调用通道
     */
    private String callLlm(String systemPrompt, String userMessage) {
        if ("mock-api-key".equals(apiKey)) {
            return "{\"content\": \"(Mock LLM Response) 请配置真实的 API Key 以激活 AI 能力。\"}";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", new Object[]{
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        });

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiEndpoint, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("LLM 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * Text2SQL: 将自然语言转换为 SQL
     */
    public Map<String, Object> textToSql(String prompt, String dbType) {
        String sysPrompt = "你是一个精通 " + dbType + " 的高级数据库工程师。请将用户的自然语言意图转换为 SQL 语句。请以 JSON 格式返回，包含 'sql' 和 'explanation' 两个字段。";
        String response = callLlm(sysPrompt, prompt);

        // Mock fallback if unconfigured
        if (response.contains("Mock LLM Response")) {
            Map<String, Object> result = new HashMap<>();
            result.put("sql", "SELECT * FROM dual; /* 请配置真实模型 */");
            result.put("explanation", response);
            return result;
        }

        try {
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            return Map.of("sql", "", "explanation", response);
        }
    }

    /**
     * 智能 SQL 优化建议
     */
    public String optimizeSql(String sql) {
        String sysPrompt = "你是一位资深的 DBA。请对以下 SQL 进行性能审查，指出其中可能存在的全表扫描、隐式转换、死锁风险或不规范的写法，并给出具体的优化建议。";
        return callLlm(sysPrompt, sql);
    }

    /**
     * SQL 解释
     */
    public String explainSql(String sql) {
        String sysPrompt = "你是一位数据库技术专家。请通俗易懂地解释以下 SQL 语句的业务意图和执行逻辑。";
        return callLlm(sysPrompt, sql);
    }

    /**
     * SQL 重写
     */
    public String rewriteSql(String sql) {
        String sysPrompt = "你是一位数据库开发专家。请将以下 SQL 重写为更加规范、性能更好的等价 SQL，并附带重写理由。只返回重写后的 SQL 和简短注释即可。";
        return callLlm(sysPrompt, sql);
    }

    /**
     * SQL 风险分析
     */
    public String riskAnalyzeSql(String sql) {
        String sysPrompt = "你是一位数据库安全专家。请对以下 SQL 进行严格的安全与风险审计，指出是否存在 SQL 注入风险、越权访问、删除全表、无 WHERE 条件更新等高危操作。";
        return callLlm(sysPrompt, sql);
    }

    /**
     * 执行计划解释
     */
    public String explainExecutionPlan(String planStr) {
        String sysPrompt = "你是一位数据库性能调优专家。用户会提供一段数据库执行计划（EXPLAIN 输出），请用通俗的语言解释其执行过程，指出其中的性能瓶颈（如 filesort, temporary table, 全表扫描等），并给出优化建议。";
        return callLlm(sysPrompt, planStr);
    }

    /**
     * 异常诊断与故障分析
     */
    public String diagnoseError(String errorLog) {
        String sysPrompt = "你是一个数据库故障排查专家。请分析这段数据库错误日志，直接指出根本原因（如死锁、OOM、网络分区等），并给出 1-3 条实操性强的解决建议。";
        return callLlm(sysPrompt, errorLog);
    }

    /**
     * 数据库巡检 (AI DBA)
     */
    public String inspectDatabase(String metricsJson) {
        String sysPrompt = "你是一个 AI DBA。以下是一份数据库实时监控指标（连接数、CPU、慢查询等）。请进行深度分析，评估当前健康度，并预测未来是否存在容量瓶颈。";
        return callLlm(sysPrompt, metricsJson);
    }
}
