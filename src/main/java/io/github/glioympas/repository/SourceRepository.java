package io.github.glioympas.repository;

import io.github.glioympas.domain.Source;
import org.jdbi.v3.core.Jdbi;

import java.util.List;
import java.util.Optional;

/**
 * Data access for the `sources` table.
 */
public class SourceRepository {

    private final Jdbi jdbi;

    public SourceRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /**
     * Inserts a source if its slug doesn't exist yet, or updates the
     * name/url/type if it does. Returns nothing; call findBySlug to read back.
     */
    public void upsert(String slug, String name, String url, String type, String itemSelector) {
        jdbi.useHandle(handle ->
                handle.createUpdate("""
                INSERT INTO sources (slug, name, url, type, item_selector)
                VALUES (:slug, :name, :url, :type, :itemSelector)
                ON CONFLICT (slug) DO UPDATE SET
                    name          = EXCLUDED.name,
                    url           = EXCLUDED.url,
                    type          = EXCLUDED.type,
                    item_selector = EXCLUDED.item_selector
                """)
                        .bind("slug", slug)
                        .bind("name", name)
                        .bind("url", url)
                        .bind("type", type)
                        .bind("itemSelector", itemSelector)
                        .execute()
        );
    }

    public Optional<Source> findBySlug(String slug) {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM sources WHERE slug = :slug")
                        .bind("slug", slug)
                        .map(new SourceMapper())
                        .findOne()
        );
    }

    public List<Source> findAll() {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT * FROM sources ORDER BY id")
                        .map(new SourceMapper())
                        .list()
        );
    }

    public void markFetched(int sourceId) {
        jdbi.useHandle(handle ->
                handle.createUpdate("UPDATE sources SET last_fetched_at = now() WHERE id = :id")
                        .bind("id", sourceId)
                        .execute()
        );
    }
}