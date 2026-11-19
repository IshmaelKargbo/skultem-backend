-- What a teacher did about a pupil the class flagged as needing attention (low attendance or marks):
-- called the guardian, met them, spoke with the pupil... with a note, and whether it's resolved. The
-- latest one per pupil is what the class page shows; the rest is the history.
CREATE TABLE IF NOT EXISTS student_follow_ups (
    id varchar(255) PRIMARY KEY,
    school_id varchar(255) NOT NULL,
    student_id varchar(255) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    class_id varchar(255) NOT NULL,
    action varchar(32) NOT NULL,
    note text,
    resolved boolean NOT NULL DEFAULT false,
    recorded_by_user_id varchar(255),
    recorded_by_name varchar(255),
    created_at timestamp(6) with time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_student_follow_ups_class ON student_follow_ups(school_id, class_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_student_follow_ups_student ON student_follow_ups(school_id, student_id);
