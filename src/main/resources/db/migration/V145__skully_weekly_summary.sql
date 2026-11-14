-- Skully's weekly summary to parents (attendance, approved marks, behaviour notes), on by default like the reminders.
ALTER TABLE skully_settings ADD COLUMN weekly_summary boolean NOT NULL DEFAULT true;
