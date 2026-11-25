-- Birth-year bounds of an age event (#88): the oldest year allowed (youth, playing up is
-- allowed) and the most recent one (veterans). Checked by registrations.
ALTER TABLE event
    ADD COLUMN min_birth_year INTEGER,
    ADD COLUMN max_birth_year INTEGER,
    ADD CONSTRAINT event_birth_year_bounds_check
        CHECK (min_birth_year IS NULL OR max_birth_year IS NULL OR min_birth_year <= max_birth_year);
