-- Score ranges the school maps to a ready-made report card remark ("80-100 -> Excellent work"),
-- kept as a JSON array of {minScore, maxScore, remark} like the school's grading scale. A card's
-- remark for its average is picked from here; the class master's own remark is written on top.
ALTER TABLE public.report_card_settings ADD COLUMN remark_scale text;
