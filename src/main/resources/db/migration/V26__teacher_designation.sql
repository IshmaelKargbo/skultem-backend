-- Free-text job title/role, e.g. "Mathematics Teacher", "Cleaner", "Security Guard", "Cook" -
-- the Teacher record has doubled as the school's general staff record since payroll, salary
-- structures and attendance all key off it, but until now nothing on the record said what a
-- given staff member's job actually was. Nullable/optional: existing rows and staff who don't
-- need a label just leave it blank.
ALTER TABLE public.teachers ADD COLUMN designation character varying(150);
