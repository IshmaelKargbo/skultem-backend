-- Show the student's photo on report cards (on by default; a school can turn it off in Report Card Design).
ALTER TABLE report_card_settings ADD COLUMN IF NOT EXISTS show_student_photo boolean NOT NULL DEFAULT true;
