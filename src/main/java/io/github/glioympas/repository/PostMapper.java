package io.github.glioympas.repository;

import io.github.glioympas.domain.Post;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

/**
 * Maps a row from the `posts` table into a Post record.
 */
public class PostMapper implements RowMapper<Post> {

    @Override
    public Post map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new Post(
                rs.getInt("id"),
                rs.getInt("source_id"),
                rs.getString("external_id"),
                rs.getString("title"),
                rs.getString("url"),
                getInstant(rs, "published_at"),
                getInstant(rs, "notified_at"),
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