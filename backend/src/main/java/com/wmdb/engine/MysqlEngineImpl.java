package com.wmdb.engine;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.wmdb.model.DbInstance;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MysqlEngineImpl implements DbEnginePlugin {

    @Override
    public void preCheck(String script) {
        com.alibaba.druid.DbType dbType = com.alibaba.druid.DbType.mysql;
        List<SQLStatement> statements = SQLUtils.parseStatements(script, dbType);

        for (SQLStatement stmt : statements) {
            if (stmt instanceof SQLUpdateStatement) {
                SQLUpdateStatement updateStmt = (SQLUpdateStatement) stmt;
                if (updateStmt.getWhere() == null) {
                    throw new RuntimeException("AST Error: UPDATE statement without WHERE clause is not allowed.");
                }
            } else if (stmt instanceof SQLDeleteStatement) {
                SQLDeleteStatement deleteStmt = (SQLDeleteStatement) stmt;
                if (deleteStmt.getWhere() == null) {
                    throw new RuntimeException("AST Error: DELETE statement without WHERE clause is not allowed.");
                }
            } else if (stmt instanceof SQLSelectStatement) {
                // A very basic check for "SELECT *"
                String stmtString = SQLUtils.toSQLString(stmt, dbType).toUpperCase();
                if (stmtString.contains("SELECT *")) {
                    throw new RuntimeException("AST Error: SELECT * is not allowed.");
                }
            }
        }
    }

    @Override
    public void execute(DbInstance instance, String script) {
        // Implementation for execution (to be handled dynamically in TicketService/Flowable callback)
        System.out.println("Executing script on instance: " + instance.getName());
    }
}
