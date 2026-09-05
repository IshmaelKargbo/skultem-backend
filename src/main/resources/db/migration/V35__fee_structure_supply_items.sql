-- A supply fee can now bundle several materials - e.g. a Uniform fee carrying the Uniform itself,
-- a House Colour, and a Necktie, each its own line with its own quantity - instead of being
-- limited to exactly one material_id/total_supply pair. See RecordPaymentUseCase#processSupply,
-- which issues one Supply record per line once the fee is fully paid.
CREATE TABLE public.fee_structure_supply_items (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    fee_structure_id character varying(255) NOT NULL,
    material_id character varying(255) NOT NULL,
    quantity integer NOT NULL,
    CONSTRAINT fee_structure_supply_items_pkey PRIMARY KEY (id)
);

ALTER TABLE ONLY public.fee_structure_supply_items
    ADD CONSTRAINT fee_structure_supply_items_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);
ALTER TABLE ONLY public.fee_structure_supply_items
    ADD CONSTRAINT fee_structure_supply_items_fee_structure_id_fkey FOREIGN KEY (fee_structure_id) REFERENCES public.fee_structures(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.fee_structure_supply_items
    ADD CONSTRAINT fee_structure_supply_items_material_id_fkey FOREIGN KEY (material_id) REFERENCES public.materials(id);
CREATE INDEX idx_fee_structure_supply_items_fee_structure_id ON public.fee_structure_supply_items (fee_structure_id);

-- Carry forward any existing single material/quantity as the first (only) line of its fee, so a
-- school that already has supply fees set up doesn't lose them.
INSERT INTO public.fee_structure_supply_items (id, school_id, fee_structure_id, material_id, quantity)
SELECT gen_random_uuid()::text, school_id, id, material_id, total_supply
FROM public.fee_structures
WHERE has_supply = true AND material_id IS NOT NULL;

ALTER TABLE public.fee_structures
    DROP COLUMN material_id,
    DROP COLUMN total_supply;
