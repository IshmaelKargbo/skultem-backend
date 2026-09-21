-- A SYSTEM_ADMIN is platform-wide and isn't "of" any school (PermissionService.isSystemAdmin grants
-- access off the role alone), so bootstrapping one must not require a school to exist yet.
ALTER TABLE school_users ALTER COLUMN school_id DROP NOT NULL;
