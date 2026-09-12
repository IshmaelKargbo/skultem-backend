-- Notification.Type.SUPPLY (parents are notified once their child's material - fee-entitled or
-- bought outright - is actually collected, see SupplyMaterialUseCase). Added directly alongside
-- the Java enum change this time, having been burned once already (V41) by adding a value to an
-- enum backed by a DB check constraint without updating the constraint in the same breath.
ALTER TABLE public.notifications DROP CONSTRAINT notifications_type_check;
ALTER TABLE public.notifications ADD CONSTRAINT notifications_type_check
    CHECK (((type)::text = ANY ((ARRAY['ATTENDANCE'::character varying, 'ANNOUNCEMENT'::character varying, 'REMINDER'::character varying, 'BEHAVIOUR'::character varying, 'FEE'::character varying, 'ASSESSMENT'::character varying, 'SUPPLY'::character varying])::text[])));
