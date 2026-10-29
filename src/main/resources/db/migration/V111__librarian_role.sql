-- LIBRARIAN: a staff member who runs the school library (books, loans, library settings) and
-- sees only that part of the portal. See Role.LIBRARIAN and LibraryController.
ALTER TABLE public.school_users
DROP CONSTRAINT school_users_role_check;

ALTER TABLE public.school_users
ADD CONSTRAINT school_users_role_check CHECK (
    role IN (
        'SYSTEM_ADMIN',
        'OWNER',
        'PROPRIETOR',
        'PRINCIPAL',
        'SUPER_ADMIN',
        'ADMIN',
        'ACCOUNTANT',
        'LIBRARIAN',
        'TEACHER',
        'PARENT'
    )
);
