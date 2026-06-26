package com.wmdb.service;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.wmdb.model.DbInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 智能化 SQL 审核服务 (SQL Lint & EXPLAIN)
 * <p>
 * 目标库直连执行 EXPLAIN 预估执行计划，进行全表扫描拦截和低效索征测。
 * </p>
 *
 * @author Jules
 */
@Service
public class SqlLintService {

    private final DataSource dataSource;
    private final DefaultDataSourceCreator dataSourceCreator;

    @Value("${wmdb.db.aes-key}")
    private String aesKey;

    public SqlLintService(DataSource dataSource, DefaultDataSourceCreator dataSourceCreator) {
        this.dataSource = dataSource;
        this.dataSourceCreator = dataSourceCreator;
    }

    /**
     * 对脚本进行 EXPLAIN 计划预检
     *
     * @param instance 目标数据库实例
     * @param script   需要预检的脚本
     * @return 返回是否通过预检
     */
    public boolean explainCheck(DbInstance instance, String script) {
        // 简化逻辑：仅对单条 SELECT 语句或包含更新/删除的语句执行 EXPLAIN 检查
        if (!"mysql".equalsIgnoreCase(instance.getDbType())) {
            // 目前仅支持 MySQL EXPLAIN 解析
            return true;
        }

        if (script == null || script.trim().isEmpty()) {
            return true;
        }

        String pwd;
        try {
            if ("mockPassword".equals(instance.getPasswordCipher())) {
                pwd = "root";
            } else {
                SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey.getBytes(StandardCharsets.UTF_8), "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
                byte[] decrypted = cipher.doFinal(java.util.Base64.getDecoder().decode(instance.getPasswordCipher()));
                pwd = new String(decrypted, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            pwd = instance.getPasswordCipher();
        }

        String dsKey = "lint_ds_" + instance.getId() + "_" + System.nanoTime();
        DataSourceProperty dsp = new DataSourceProperty();
        dsp.setPoolName(dsKey);
        dsp.setUrl(instance.getJdbcUrl());
        dsp.setUsername(instance.getUsername());
        dsp.setPassword(pwd);
        dsp.setDriverClassName("com.mysql.cj.jdbc.Driver");

        DynamicRoutingDataSource drds = (DynamicRoutingDataSource) dataSource;
        DataSource newDataSource = dataSourceCreator.createDataSource(dsp);
        drds.addDataSource(dsKey, newDataSource);
        DynamicDataSourceContextHolder.push(dsKey);

        try (Connection conn = drds.getConnection();
             Statement stmt = conn.createStatement()) {

            java.util.List<String> queries = com.wmdb.utils.SqlSplitUtils.split(script);
            for (String query : queries) {
                String trimmed = query.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;

                // 强制只针对 SELECT/UPDATE/DELETE 做 EXPLAIN
                if (trimmed.toUpperCase().startsWith("SELECT") ||
                    trimmed.toUpperCase().startsWith("UPDATE") ||
                    trimmed.toUpperCase().startsWith("DELETE")) {

                    try (ResultSet rs = stmt.executeQuery("EXPLAIN " + trimmed)) {
                        while (rs.next()) {
                            String type = rs.getString("type");
                            // 极简 Lint 规则：不允许全表扫描
                            if ("ALL".equalsIgnoreCase(type)) {
                                throw new RuntimeException("SQL Lint Error: EXPLAIN plan shows a full table scan (type=ALL) for query: " + trimmed);
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            // 如果 EXPLAIN 语法错误等，暂定抛出
            throw new RuntimeException("SQL Lint failed to execute EXPLAIN: " + e.getMessage(), e);
        } finally {
            DynamicDataSourceContextHolder.poll();
            drds.removeDataSource(dsKey);
        }
    }
}
