-- Lets a fee structure target one gender only (e.g. a boys' vs girls' uniform supply fee priced
-- differently under the same fee category/term) - null means "every gender", same optional-filter
-- shape as new_students_only/old_students_only.
ALTER TABLE public.fee_structures
    ADD COLUMN gender character varying(20),
    ADD CONSTRAINT fee_structures_gender_check CHECK (gender IS NULL OR (gender)::text = ANY ((ARRAY['MALE'::character varying, 'FEMALE'::character varying])::text[]));
