package com.wmdb.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL 在线查询返回结果封装
 *
 * @author wm
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlQueryResult {

    /**
     * 是否执行成功
     */
    private boolean success;

    /**
     * 目标数据库名称
     */
    private String databaseName;

    /**
     * 执行的 SQL
     */
    private String sql;

    /**
     * 结果集字段列名清单
     */
    @Builder.Default
    private List<String> columns = new ArrayList<>();

    /**
     * 结果集数据行（键值对）
     */
    @Builder.Default
    private List<Map<String, Object>> rows = new ArrayList<>();

    /**
     * 返回总行数
     */
    private int totalRows;

    /**
     * 执行耗时 (ms)
     */
    private long durationMs;

    /**
     * 错误信息（如有）
     */
    private String errorMessage;
}
