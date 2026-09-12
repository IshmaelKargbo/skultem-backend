-- ActivityType.SALE (added for material sales - see MaterialSaleController and friends) was
-- never added to the activities table's own type check constraint, so every activity log call
-- for a sale (create, fulfill, payment, cancel) failed at commit with a check constraint
-- violation - which, because it happens inside the same @Transactional as the sale itself,
-- silently rolled back the whole sale along with it.
ALTER TABLE public.activities DROP CONSTRAINT activities_type_check;
ALTER TABLE public.activities ADD CONSTRAINT activities_type_check
    CHECK (((type)::text = ANY (ARRAY[('STUDENT'::character varying)::text, ('TEACHER'::character varying)::text, ('PARENT'::character varying)::text, ('USER'::character varying)::text, ('CLASS'::character varying)::text, ('SUBJECT'::character varying)::text, ('GRADE'::character varying)::text, ('FEES'::character varying)::text, ('EXPENSE'::character varying)::text, ('PAYMENT'::character varying)::text, ('SCHOOL'::character varying)::text, ('SETTINGS'::character varying)::text, ('SUPPLY'::character varying)::text, ('SALE'::character varying)::text])));
