-- version column for optimistic locking; existing seats start at 0
ALTER TABLE seats ADD COLUMN version BIGINT NOT NULL DEFAULT 0;