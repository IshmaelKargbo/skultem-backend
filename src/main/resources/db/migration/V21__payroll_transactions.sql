-- Payroll now posts to the general ledger (transactions), same as Expense already does - a
-- generated payroll run is real money leaving the school and financial reports (P&L, expense
-- breakdowns) need to reflect that. Adds PAYROLL to both the transaction type and reference type
-- enums.
ALTER TABLE public.transactions DROP CONSTRAINT transactions_transaction_type_check;
ALTER TABLE public.transactions ADD CONSTRAINT transactions_transaction_type_check
    CHECK (((transaction_type)::text = ANY ((ARRAY['FEE_ASSIGNMENT'::character varying, 'PAYMENT'::character varying, 'DISCOUNT'::character varying, 'REFUND'::character varying, 'EXPENSE'::character varying, 'ADJUSTMENT'::character varying, 'PAYROLL'::character varying])::text[])));

ALTER TABLE public.transactions DROP CONSTRAINT transactions_reference_type_check;
ALTER TABLE public.transactions ADD CONSTRAINT transactions_reference_type_check
    CHECK (((reference_type)::text = ANY ((ARRAY['STUDENT'::character varying, 'EXPENSE'::character varying, 'SYSTEM'::character varying, 'OTHER'::character varying, 'PAYROLL'::character varying])::text[])));
