-- Which M-Compose design each school prints a kind of document with (see DocumentDesignService). No row = the
-- server's default design for that kind (m-compose.templates.*).
CREATE TABLE IF NOT EXISTS school_document_designs (
    school_id varchar(255) NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    kind varchar(32) NOT NULL CHECK (kind IN ('REPORT_CARD', 'EXAM_RESULT_SLIP', 'EXAM_STATEMENT')),
    template varchar(255) NOT NULL,
    updated_by varchar(255),
    updated_at timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (school_id, kind)
);
