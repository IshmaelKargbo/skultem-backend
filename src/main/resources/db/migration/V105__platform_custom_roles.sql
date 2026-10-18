-- Roles an owner creates on top of the built-in ones. A staff member on a custom role has its key (CUSTOM_<id>)
-- in platform_staff_roles.role, so that column can no longer be limited to the built-in names.
CREATE TABLE platform_custom_roles (
    id character varying(36) PRIMARY KEY,
    label character varying(60) NOT NULL UNIQUE,
    description text NOT NULL DEFAULT '',
    permissions text NOT NULL,
    created_by character varying(255) REFERENCES users (id) ON DELETE SET NULL,
    created_at timestamp(6) with time zone NOT NULL DEFAULT now()
);

ALTER TABLE platform_staff_roles DROP CONSTRAINT platform_staff_roles_role_check;
ALTER TABLE platform_staff_roles ALTER COLUMN role TYPE character varying(50);
