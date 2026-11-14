-- Skully's latest insights brief for each school's leadership (written by Gemini from the school's own reports).
CREATE TABLE skully_insights (
    school_id character varying(255) NOT NULL,
    brief text NOT NULL,
    generated_at timestamp(6) with time zone NOT NULL,
    generated_by character varying(255),
    CONSTRAINT skully_insights_pkey PRIMARY KEY (school_id)
);
