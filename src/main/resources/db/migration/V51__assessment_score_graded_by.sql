-- A subject can now have more than one teacher assigned, sharing the same gradebook - this
-- tracks which of them actually entered/last changed a given score. Nullable, no FK, same
-- pattern as attendance.recorded_by_user_id (V47/V48).
ALTER TABLE assessment_scores ADD COLUMN graded_by_user_id character varying(255);
