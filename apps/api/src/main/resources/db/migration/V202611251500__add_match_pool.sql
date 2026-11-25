-- The pool of a pool match (#93); null for a knockout match or a match out of a draw.
ALTER TABLE match
    ADD COLUMN pool_name VARCHAR(2),
    ADD CONSTRAINT match_pool_check CHECK (pool_name IS NULL OR bracket_position IS NULL);
