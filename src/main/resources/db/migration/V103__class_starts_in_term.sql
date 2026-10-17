-- A class can join the school part-way through the year (e.g. SSS 1 starts in the second term). Students of such a class
-- are not graded for the terms before it starts.
ALTER TABLE classes ADD COLUMN starts_in_term integer NOT NULL DEFAULT 1;
