-- There is no production database yet: existing events simply become open to everyone.

ALTER TABLE event
    DROP COLUMN category,
    ADD COLUMN category_type  VARCHAR(5) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN category_label VARCHAR(50);

ALTER TABLE event
    ALTER COLUMN category_type DROP DEFAULT,
    ADD CONSTRAINT event_category_check
        CHECK ((category_type = 'OPEN' AND category_label IS NULL)
            OR (category_type IN ('AGE', 'LEVEL') AND category_label IS NOT NULL));
