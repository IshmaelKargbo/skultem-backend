-- Material sales now post to the general ledger (transactions), same as Payment/Expense/Payroll
-- already do - money actually collected for a sale is real revenue and financial reports need to
-- reflect it. Adds SALE to the transaction type enum and MATERIAL_SALE to the reference type enum,
-- following the same pattern V21 used to add PAYROLL.
ALTER TABLE public.transactions DROP CONSTRAINT transactions_transaction_type_check;
ALTER TABLE public.transactions ADD CONSTRAINT transactions_transaction_type_check
    CHECK (((transaction_type)::text = ANY ((ARRAY['FEE_ASSIGNMENT'::character varying, 'PAYMENT'::character varying, 'DISCOUNT'::character varying, 'REFUND'::character varying, 'EXPENSE'::character varying, 'ADJUSTMENT'::character varying, 'PAYROLL'::character varying, 'SALE'::character varying])::text[])));

ALTER TABLE public.transactions DROP CONSTRAINT transactions_reference_type_check;
ALTER TABLE public.transactions ADD CONSTRAINT transactions_reference_type_check
    CHECK (((reference_type)::text = ANY ((ARRAY['STUDENT'::character varying, 'EXPENSE'::character varying, 'SYSTEM'::character varying, 'OTHER'::character varying, 'PAYROLL'::character varying, 'MATERIAL_SALE'::character varying])::text[])));
