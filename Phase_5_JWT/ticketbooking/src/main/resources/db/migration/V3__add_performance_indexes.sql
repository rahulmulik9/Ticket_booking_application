-- trigram support, needed for fast LIKE '%text%' searches
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- name search: matches the lower(title) LIKE lower('%...%') query that Spring Data generates
CREATE INDEX idx_movies_title_trgm ON movies USING gin (lower(title) gin_trgm_ops);

-- shows of a movie: Postgres does not index foreign keys automatically
CREATE INDEX idx_shows_movie_id ON shows (movie_id);