-- Self-service "forgot password". One row per reset link sent. Only a SHA-256 hash of the token is stored; the token
-- itself exists only in the email. A link works once, until it expires or a newer link replaces it.
CREATE TABLE public.password_reset_tokens (
    id character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL,
    token_hash character varying(64) NOT NULL,
    -- The school the request came from (its address is in the link).
    school_id character varying(255),
    expires_at timestamp(6) with time zone NOT NULL,
    used_at timestamp(6) with time zone,
    -- Set when a newer link replaces this one, so an older email's link stops working.
    revoked_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT password_reset_tokens_hash_key UNIQUE (token_hash),
    CONSTRAINT password_reset_tokens_user_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_user_created ON public.password_reset_tokens (user_id, created_at);
