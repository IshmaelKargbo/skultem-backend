ALTER TABLE academic_years
    ADD COLUMN next_year VARCHAR(255);

ALTER TABLE academic_years
    ADD CONSTRAINT fk_academic_years_next_year FOREIGN KEY (next_year) REFERENCES academic_years (id);

ALTER TABLE academic_years
    ADD CONSTRAINT uq_academic_years_next_year UNIQUE (next_year);
