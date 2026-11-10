-- Devices that turned on native notifications (Web Push). One row per browser/app install; a user can have several.
CREATE TABLE IF NOT EXISTS push_subscriptions (
    id varchar(255) PRIMARY KEY,
    user_id varchar(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    school_id varchar(255),
    endpoint text NOT NULL UNIQUE,
    p256dh varchar(255) NOT NULL,
    auth varchar(255) NOT NULL,
    user_agent varchar(512),
    created_at timestamp(6) with time zone NOT NULL,
    last_used_at timestamp(6) with time zone
);
CREATE INDEX IF NOT EXISTS idx_push_subscriptions_user ON push_subscriptions(user_id);
