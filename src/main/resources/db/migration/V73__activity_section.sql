-- The management section an activity happened in (set when a section-limited admin does it), so the dashboard's Recent
-- Activity can show a section admin only their own section's activity.
ALTER TABLE activities ADD COLUMN management_section_id VARCHAR(255);
CREATE INDEX idx_activities_section ON activities (school_id, management_section_id);
