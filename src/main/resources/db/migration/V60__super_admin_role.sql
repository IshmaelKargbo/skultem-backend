-- SUPER_ADMIN: a staff member with access to the whole school portal (see PermissionService).
ALTER TABLE public.school_users DROP CONSTRAINT school_users_role_check;
ALTER TABLE public.school_users ADD CONSTRAINT school_users_role_check CHECK (role IN
    ('SYSTEM_ADMIN', 'OWNER', 'PROPRIETOR', 'SUPER_ADMIN', 'ADMIN', 'ACCOUNTANT', 'TEACHER', 'PARENT'));
