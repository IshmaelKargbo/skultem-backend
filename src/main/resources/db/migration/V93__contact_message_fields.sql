-- The marketing contact form asks for: full name, work email, school name, phone number and the main
-- challenge - so a message has a school and a challenge instead of a subject and a free-form message.
ALTER TABLE marketing_contact_messages ADD COLUMN school character varying(255);
ALTER TABLE marketing_contact_messages RENAME COLUMN message TO challenge;
ALTER TABLE marketing_contact_messages DROP COLUMN subject;
