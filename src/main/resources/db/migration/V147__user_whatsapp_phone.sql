-- A user's WhatsApp number (digits with the country code), so Skully can recognise school staff.
ALTER TABLE users ADD COLUMN phone character varying(32);
CREATE UNIQUE INDEX uq_users_phone ON users (phone) WHERE phone IS NOT NULL;
