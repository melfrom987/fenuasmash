ALTER TABLE event
    ADD COLUMN planned_date TIMESTAMPTZ,
    ADD COLUMN max_entries  INTEGER,
    ADD CONSTRAINT event_max_entries_check CHECK (max_entries BETWEEN 2 AND 256);
