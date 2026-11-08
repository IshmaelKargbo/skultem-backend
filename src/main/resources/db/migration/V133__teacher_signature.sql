-- Teachers keep their own signature (drawn or uploaded on their profile). When a class teacher approves their
-- class's report cards, their name and signature are copied onto each card - approving is signing - so a card
-- keeps the signature it was approved with even if the teacher changes it later.
ALTER TABLE public.teachers ADD COLUMN signature character varying(512);

ALTER TABLE public.report_cards
    ADD COLUMN class_teacher_name character varying(255),
    ADD COLUMN class_teacher_signature character varying(512);
