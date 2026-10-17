-- Platform-wide settings the system admin can change. Only keys the application knows about are accepted (see
-- PlatformSettingsService); a key with no row here simply uses its built-in default.
CREATE TABLE platform_config (
    key character varying(100) PRIMARY KEY,
    value text NOT NULL,
    updated_by character varying(255) REFERENCES users (id) ON DELETE SET NULL,
    updated_at timestamp(6) with time zone
);

-- What each system admin may do on the platform. A system admin with no row here is an OWNER (full access), so
-- nobody loses access when this is introduced; restricting someone means giving them a narrower role.
CREATE TABLE platform_staff_roles (
    user_id character varying(255) PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    role character varying(20) NOT NULL,
    updated_by character varying(255) REFERENCES users (id) ON DELETE SET NULL,
    updated_at timestamp(6) with time zone,
    CONSTRAINT platform_staff_roles_role_check CHECK (role IN ('OWNER', 'BILLING', 'MARKETING', 'SUPPORT', 'AUDITOR'))
);
