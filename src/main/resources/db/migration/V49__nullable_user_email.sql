-- A parent can be enrolled without an email (not every parent has one). The email is added by an
-- admin later, which is also what grants the parent portal access since login is keyed on email.
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
