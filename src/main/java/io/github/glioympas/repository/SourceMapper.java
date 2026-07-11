package io.github.glioympas.repository;

import io.github.glioympas.domain.Source;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Maps a row from the `sources` table into a Source record.
 */
public class SourceMapper implements RowMapper<Source> {

    @Override
    public Source map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new Source(
                rs.getInt("id"),
                rs.getString("slug"),
                rs.getString("name"),
                rs.getString("url"),
                rs.getString("type"),
                rs.getString("item_selector"),
                getInstant(rs, "last_fetched_at"),
                getInstant(rs, "created_at")
        );
    }

    /**
     * Reads a timestamp column that may be null.
     */
    private static Instant getInstant(ResultSet rs, String column) throws SQLException {
        var ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toInstant();
    }
}