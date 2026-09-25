-- Per-management-section branding. A school run in sections (e.g. Primary / Secondary) can be
-- housed in different places, with a different head and crest for each. Every column is optional:
-- null means "use the school's own value", so existing schools and any section that hasn't set
-- its own keep rendering exactly what they do today.
ALTER TABLE public.management_sections
    ADD COLUMN logo TEXT,
    ADD COLUMN principal_name VARCHAR(255),
    ADD COLUMN principal_signature TEXT,
    ADD COLUMN address JSONB;
