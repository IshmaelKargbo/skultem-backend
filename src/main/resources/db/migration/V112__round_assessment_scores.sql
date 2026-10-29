-- Whether assessment scores and averages are shown as whole numbers (26.7 -> 27) or with one decimal (26.7).
-- On by default so existing schools see no change until they turn it off.
ALTER TABLE public.schools
    ADD COLUMN round_assessment_scores boolean NOT NULL DEFAULT true;
