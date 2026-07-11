CREATE TABLE sources (
                         id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         slug            TEXT NOT NULL UNIQUE,
                         name            TEXT NOT NULL,
                         url             TEXT NOT NULL,
                         type            TEXT NOT NULL DEFAULT 'rss',
                         last_fetched_at TIMESTAMPTZ,
                         created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE posts (
                       id            INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       source_id     INT NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
                       external_id   TEXT NOT NULL,
                       title         TEXT NOT NULL,
                       url           TEXT NOT NULL,
                       published_at  TIMESTAMPTZ,
                       notified_at   TIMESTAMPTZ,
                       created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

                       CONSTRAINT uq_posts_source_external UNIQUE (source_id, external_id)
);

CREATE INDEX idx_posts_source_id ON posts (source_id);
CREATE INDEX idx_posts_notified_at ON posts (notified_at);