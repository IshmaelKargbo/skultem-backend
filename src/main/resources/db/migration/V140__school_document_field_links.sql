-- How a school links an M-Compose design's placeholders to Skultem's data (see DocumentFieldLinkService), per kind of
-- document and design. links is JSON: {"placeholder": {"source": "key"} | {"text": "fixed"} | {"source": "results",
-- "columns": {"column": "row key" | "text:fixed"}}}. A placeholder without a link gets the data of the same name.
CREATE TABLE IF NOT EXISTS school_document_field_links (
    school_id varchar(255) NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    kind varchar(32) NOT NULL CHECK (kind IN ('REPORT_CARD', 'EXAM_RESULT_SLIP', 'EXAM_STATEMENT')),
    template varchar(255) NOT NULL,
    links text NOT NULL,
    updated_by varchar(255),
    updated_at timestamp(6) with time zone NOT NULL,
    PRIMARY KEY (school_id, kind, template)
);
