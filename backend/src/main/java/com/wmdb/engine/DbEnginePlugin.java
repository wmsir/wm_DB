package com.wmdb.engine;

import com.wmdb.model.DbInstance;

public interface DbEnginePlugin {
    void preCheck(String script);
    void execute(DbInstance instance, String script);
}
