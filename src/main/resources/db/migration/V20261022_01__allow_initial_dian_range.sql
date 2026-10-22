-- A newly configured range starts one number before range_from so the first
-- allocation returns exactly range_from instead of skipping it.
ALTER TABLE dian_numbering_range
    DROP CONSTRAINT IF EXISTS ck_dian_range_values;

ALTER TABLE dian_numbering_range
    ADD CONSTRAINT ck_dian_range_values
    CHECK (range_from - 1 <= current_number AND current_number <= range_to);
