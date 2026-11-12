-- Which of a design's groups (elements shown or hidden together, e.g. "Position in class") a school shows or hides,
-- as JSON {groupId: true|false}. A group not listed follows the school's report card settings, else the design.
ALTER TABLE school_document_field_links ADD COLUMN IF NOT EXISTS groups text;
