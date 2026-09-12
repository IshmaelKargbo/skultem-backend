-- A sale no longer deducts stock itself - every sale is now backed by a Supply record, the one
-- place collection (and therefore stock deduction) actually happens, exactly like a fee-entitled
-- Supply already worked. This lets a Supply be linked to a walk-in buyer (student_id nullable,
-- customer_name added, mirroring material_sales) and traces a Supply back to the sale that created
-- it (source_sale_id) so GetPendingPickupsUseCase and CancelMaterialSaleUseCase can find it.
ALTER TABLE public.supplies
    ALTER COLUMN student_id DROP NOT NULL,
    ADD COLUMN customer_name character varying(150),
    ADD COLUMN source_sale_id character varying(255);

ALTER TABLE public.supplies
    ADD CONSTRAINT supplies_buyer_check CHECK (student_id IS NOT NULL OR customer_name IS NOT NULL);

ALTER TABLE ONLY public.supplies
    ADD CONSTRAINT supplies_source_sale_id_fkey FOREIGN KEY (source_sale_id) REFERENCES public.material_sales(id);

CREATE INDEX idx_supplies_source_sale_id ON public.supplies (source_sale_id);

-- Nullable: existing sales recorded before this link existed (there are only a handful of test
-- rows, already FULFILLED) have no linked Supply to point to and aren't backfilled one - they're
-- already resolved, so there's nothing left for that link to do for them. Every new sale always
-- creates and sets one - see CreateMaterialSaleUseCase.
ALTER TABLE public.material_sales
    ADD COLUMN supply_id character varying(255);

ALTER TABLE ONLY public.material_sales
    ADD CONSTRAINT material_sales_supply_id_fkey FOREIGN KEY (supply_id) REFERENCES public.supplies(id);

CREATE INDEX idx_material_sales_supply_id ON public.material_sales (supply_id);
