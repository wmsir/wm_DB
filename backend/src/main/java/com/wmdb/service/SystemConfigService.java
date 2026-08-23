package com.wmdb.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 平台全局安全与审计策略配置服务
 * <p>
 * 管理员可在线动态配置：事务级预执行校验强制策略、DML/DDL 数据备份必填策略、最大查询行数限制等。
 * </p>
 *
 * @author wm
 */
@Slf4j
@Service
public class SystemConfigService {

    private final ConcurrentHashMap<String, Object> configStore = new ConcurrentHashMap<>();

    public SystemConfigService() {
        // 默认策略配置
        configStore.put("enforceDryRun", true);
        configStore.put("requireBackup", true);
        configStore.put("maxQueryRows", 1000);
        configStore.put("allowDbaManualTool", true);
    }

    public boolean isEnforceDryRun() {
        Object val = configStore.get("enforceDryRun");
        return val instanceof Boolean ? (Boolean) val : Boolean.parseBoolean(String.valueOf(val));
    }

    public boolean isRequireBackup() {
        Object val = configStore.get("requireBackup");
        return val instanceof Boolean ? (Boolean) val : Boolean.parseBoolean(String.valueOf(val));
    }

    public int getMaxQueryRows() {
        Object val = configStore.get("maxQueryRows");
        return val instanceof Integer ? (Integer) val : 1000;
    }

    public SafetyPolicyDTO getSafetyPolicies() {
        return SafetyPolicyDTO.builder()
                .enforceDryRun(isEnforceDryRun())
                .requireBackup(isRequireBackup())
                .maxQueryRows(getMaxQueryRows())
                .build();
    }

    public void updateSafetyPolicies(SafetyPolicyDTO dto) {
        if (dto != null) {
            if (dto.getEnforceDryRun() != null) {
                configStore.put("enforceDryRun", dto.getEnforceDryRun());
            }
            if (dto.getRequireBackup() != null) {
                configStore.put("requireBackup", dto.getRequireBackup());
            }
            if (dto.getMaxQueryRows() != null) {
                configStore.put("maxQueryRows", dto.getMaxQueryRows());
            }
            log.info("Updated safety policies: enforceDryRun={}, requireBackup={}, maxQueryRows={}",
                    isEnforceDryRun(), isRequireBackup(), getMaxQueryRows());
        }
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SafetyPolicyDTO {
        private Boolean enforceDryRun;
        private Boolean requireBackup;
        private Integer maxQueryRows;
    }
}