-- Point-of-sale style sales for school materials (uniforms, socks, textbooks, ...) - independent
-- of the fee-entitled `supplies` table. A sale may or may not be linked to a student (a walk-in
-- buyer is recorded via customer_name instead), and schools routinely pre-sell items not yet in
-- stock: such a sale is created PENDING_SUPPLY ("settle later") without touching stock, and is
-- moved to FULFILLED - deducting stock only then - once the item is actually handed over.
CREATE TABLE public.material_sales (
    id character varying(255) NOT NULL,
    school_id character varying(255) NOT NULL,
    student_id character varying(255),
    customer_name character varying(150),
    material_id character varying(255) NOT NULL,
    quantity integer NOT NULL,
    unit_price numeric(19,2) NOT NULL,
    total_amount numeric(19,2) NOT NULL,
    amount_paid numeric(19,2) NOT NULL DEFAULT 0,
    payment_method character varying(255),
    note character varying(255),
    status character varying(255) NOT NULL,
    fulfilled_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    CONSTRAINT material_sales_pkey PRIMARY KEY (id),
    CONSTRAINT material_sales_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_SUPPLY'::character varying, 'FULFILLED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT material_sales_payment_method_check CHECK (payment_method IS NULL OR ((payment_method)::text = ANY ((ARRAY['CASH'::character varying, 'BANK'::character varying, 'MOBILE_MONEY'::character varying])::text[]))),
    CONSTRAINT material_sales_buyer_check CHECK (student_id IS NOT NULL OR customer_name IS NOT NULL)
);

ALTER TABLE ONLY public.material_sales
    ADD CONSTRAINT material_sales_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id);
ALTER TABLE ONLY public.material_sales
    ADD CONSTRAINT material_sales_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id);
ALTER TABLE ONLY public.material_sales
    ADD CONSTRAINT material_sales_material_id_fkey FOREIGN KEY (material_id) REFERENCES public.materials(id);

CREATE INDEX idx_material_sales_school_id ON public.material_sales (school_id);
CREATE INDEX idx_material_sales_student_id ON public.material_sales (student_id);
CREATE INDEX idx_material_sales_material_id ON public.material_sales (material_id);
CREATE INDEX idx_material_sales_status ON public.material_sales (status);
