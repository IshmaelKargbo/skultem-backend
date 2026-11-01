-- Each payslip keeps its own copy of the allowance/deduction lines (name + resolved amount) taken
-- when the payroll run is created, so a payslip can show "Housing 300,000, Transport 50,000, ..."
-- and stay accurate even if the salary is edited later. Payslips from before this have no lines;
-- they still show their totals.
CREATE TABLE public.payslip_allowance_lines (
    payslip_id character varying(255) NOT NULL REFERENCES public.payslips(id) ON DELETE CASCADE,
    name character varying(255) NOT NULL,
    amount numeric(19,2) NOT NULL
);
CREATE INDEX idx_payslip_allowance_lines_payslip_id ON public.payslip_allowance_lines (payslip_id);

CREATE TABLE public.payslip_deduction_lines (
    payslip_id character varying(255) NOT NULL REFERENCES public.payslips(id) ON DELETE CASCADE,
    name character varying(255) NOT NULL,
    amount numeric(19,2) NOT NULL
);
CREATE INDEX idx_payslip_deduction_lines_payslip_id ON public.payslip_deduction_lines (payslip_id);
