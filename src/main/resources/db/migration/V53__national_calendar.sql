-- Platform-wide ("ministry") academic calendar, maintained by a SYSTEM_ADMIN - see
-- NationalAcademicYear. A new school gets a copy of the current one as its own academic year and
-- terms (see ProvisionAcademicCalendarForNewSchoolUseCase); nothing here links back to a school.
CREATE TABLE national_academic_years (
    id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    is_current boolean NOT NULL DEFAULT false,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT national_academic_years_pkey PRIMARY KEY (id),
    CONSTRAINT uq_national_academic_years_name UNIQUE (name)
);

CREATE TABLE national_terms (
    id character varying(255) NOT NULL,
    national_academic_year_id character varying(255) NOT NULL,
    term_number integer NOT NULL,
    name character varying(255) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    CONSTRAINT national_terms_pkey PRIMARY KEY (id),
    CONSTRAINT uq_national_terms_year_number UNIQUE (national_academic_year_id, term_number),
    CONSTRAINT fk_national_terms_year FOREIGN KEY (national_academic_year_id)
        REFERENCES national_academic_years(id) ON DELETE CASCADE
);
