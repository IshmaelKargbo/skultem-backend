-- Profile photo, shared across every role a User account holds (Teacher, Parent, and a plain
-- Admin/Accountant/Proprietor account) since it's a property of the person, not any one role.
-- Nullable and optional - can be added any time after the account is created.
ALTER TABLE public.users ADD COLUMN photo character varying(500);
