-- A school's design for one level of a kind of document (a Primary report card, a JSS one), when it wants separate
-- ones. A level without a row uses the school's general design (school_document_designs), else the server default.
CREATE TABLE IF NOT EXISTS school_level_document_designs (
    school_id varchar(255) NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    kind varchar(32) NOT NULL CHECK (kind IN ('REPORT_CARD', 'EXAM_RESULT_SLIP', 'EXAM_STATEMENT')),
    level varchar(32) NOT NULL,
    template varchar(255) NOT NULL,
    updated_by varchar(255),
    updated_at timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (school_id, kind, level)
);
