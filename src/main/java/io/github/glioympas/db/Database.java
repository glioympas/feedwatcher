package io.github.glioympas.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.glioympas.config.AppConfig;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;

import javax.sql.DataSource;

/**
 * Owns the connection pool, runs migrations, and exposes a JDBI instance.
 * Create one of these at startup and share it across the app.
 */
public final class Database implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final Jdbi jdbi;

    public Database(AppConfig.DatabaseConfig config) {
        this.dataSource = buildDataSource(config);
        this.jdbi = Jdbi.create(dataSource).installPlugin(new PostgresPlugin());
    }

    private static HikariDataSource buildDataSource(AppConfig.DatabaseConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.url());
        hikari.setUsername(config.username());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(config.maxPoolSize());
        hikari.setPoolName("feedwatcher-pool");
        return new HikariDataSource(hikari);
    }

    /**
     * Runs any pending Flyway migrations. Call once at startup,
     * before any queries.
     */
    public void migrate() {
        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();
    }

    public Jdbi jdbi() {
        return jdbi;
    }

    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}