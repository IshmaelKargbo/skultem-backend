-- Installment plans. A fee that allows installments can carry a plan: two or more parts, each with a label, an
-- amount and a due date, adding up to the fee. Payments still go against the fee as a whole; the plan decides how
-- much should have been paid by when (so when the fee is overdue) and shows each part as paid, due or overdue.
CREATE TABLE public.fee_structure_installments (
    fee_structure_id character varying(255) NOT NULL REFERENCES public.fee_structures (id) ON DELETE CASCADE,
    position         integer NOT NULL,
    label            character varying(100) NOT NULL,
    amount           numeric(14,2) NOT NULL CHECK (amount > 0),
    due_date         date NOT NULL,
    PRIMARY KEY (fee_structure_id, position)
);

-- The platform fee's plan, set per school by Skultem: BY_TERM (the default) splits it into one part per term of the
-- academic year, each due at that term's end; FULL charges it in one go. term_shares optionally weights the terms
-- ("50,30,20" percent); empty = an equal split.
ALTER TABLE public.platform_settings
    ADD COLUMN installment_mode character varying(16) NOT NULL DEFAULT 'BY_TERM',
    ADD COLUMN term_shares character varying(100),
    ADD CONSTRAINT platform_settings_installment_mode_check CHECK (installment_mode IN ('FULL', 'BY_TERM'));
