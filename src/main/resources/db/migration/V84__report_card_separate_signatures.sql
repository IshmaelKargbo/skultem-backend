-- The single "Signatures" switch becomes one per signatory, so a school can print the class teacher's
-- signature line, the principal's, both, or neither. show_signatures stays as "either is on".
ALTER TABLE public.report_card_settings ADD COLUMN show_teacher_signature boolean NOT NULL DEFAULT true;
ALTER TABLE public.report_card_settings ADD COLUMN show_principal_signature boolean NOT NULL DEFAULT true;

UPDATE public.report_card_settings
SET show_teacher_signature = show_signatures, show_principal_signature = show_signatures;
