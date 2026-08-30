package com.empresa.functions.common;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

/**
 * Pool de conexiones a Oracle como singleton estático. Azure Functions
 * reutiliza el proceso entre invocaciones "calientes" (warm start): crear
 * el pool dentro del handler de cada function agotaría las conexiones de
 * Oracle rápidamente. Ver CLAUDE.md, sección "Azure Functions".
 */
public final class DataSourceProvider {

    private static volatile HikariDataSource dataSource;

    private DataSourceProvider() {
    }

    public static DataSource get() {
        HikariDataSource result = dataSource;
        if (result == null) {
            synchronized (DataSourceProvider.class) {
                result = dataSource;
                if (result == null) {
                    dataSource = result = build();
                }
            }
        }
        return result;
    }

    private static HikariDataSource build() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv("ORACLE_JDBC_URL"));
        config.setUsername(System.getenv("ORACLE_DB_USER"));
        config.setPassword(System.getenv("ORACLE_DB_PASSWORD"));
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setPoolName("oracle-functions-pool");
        return new HikariDataSource(config);
    }
}
