-- The accountant-entered transaction reference (bank slip / mobile money ID) typed on the
-- "Record Payment" form was being captured by the API but never persisted - RecordPaymentUseCase
-- only ever wrote the system-generated receipt number into payments.reference_no. This column
-- gives that manual reference somewhere to live, separate from the receipt number.
ALTER TABLE payments ADD COLUMN external_reference VARCHAR(255);
