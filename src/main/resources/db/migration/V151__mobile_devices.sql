-- Phones running the Skultem app that agreed to notifications (Firebase Cloud Messaging). One row per app
-- install; a user can have several. The token is Firebase's address for that install.
CREATE TABLE IF NOT EXISTS mobile_devices (
    id varchar(255) PRIMARY KEY,
    user_id varchar(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    school_id varchar(255),
    token text NOT NULL UNIQUE,
    platform varchar(16) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    last_used_at timestamp(6) with time zone
);
CREATE INDEX IF NOT EXISTS idx_mobile_devices_user ON mobile_devices(user_id);
