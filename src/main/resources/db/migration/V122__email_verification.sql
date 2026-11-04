-- Email ownership verification. An account's email is "verified" once the person opens a link sent to that address.
-- Existing accounts start unverified (and keep full access - nothing is blocked on this).
ALTER TABLE public.users ADD COLUMN email_verified boolean NOT NULL DEFAULT false;
ALTER TABLE public.users ADD COLUMN email_verified_at timestamp(6) with time zone;

-- One row per verification link sent. Only a SHA-256 hash of the token is stored; the token itself exists only in the
-- email. `email` is the address the link was sent to - a link never verifies a different (later) address.
CREATE TABLE public.email_verification_tokens (
    id character varying(255) NOT NULL,
    user_id character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    token_hash character varying(64) NOT NULL,
    -- The school the request came from (for the link's address); null on the admin portal.
    school_id character varying(255),
    expires_at timestamp(6) with time zone NOT NULL,
    used_at timestamp(6) with time zone,
    -- Set when a newer link replaces this one, so an older email's link stops working.
    revoked_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT email_verification_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT email_verification_tokens_hash_key UNIQUE (token_hash),
    CONSTRAINT email_verification_tokens_user_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_tokens_user_created ON public.email_verification_tokens (user_id, created_at);
