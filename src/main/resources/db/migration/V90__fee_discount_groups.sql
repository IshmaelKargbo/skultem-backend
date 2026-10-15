-- A discount can now cover several fees (e.g. first and second term school fees) for one student.
-- Each fee still gets its own row; group_id ties the rows of one discount together so they are
-- listed, and deleted, as a unit. Existing discounts become groups of one.
ALTER TABLE fee_discounts ADD COLUMN group_id character varying(255);
UPDATE fee_discounts SET group_id = id;
ALTER TABLE fee_discounts ALTER COLUMN group_id SET NOT NULL;
CREATE INDEX idx_fee_discounts_group ON fee_discounts (school_id, group_id);

-- Discounts used to be created without being linked to the student's fee record, so the fee never
-- showed them. Link the existing ones (latest wins if a fee somehow has several).
UPDATE student_fees sf
SET discount_id = (
    SELECT d.id FROM fee_discounts d
    WHERE d.enrollment_id = sf.enrollment_id AND d.fee_id = sf.fee_id
    ORDER BY d.created_at DESC LIMIT 1)
WHERE sf.discount_id IS NULL;
