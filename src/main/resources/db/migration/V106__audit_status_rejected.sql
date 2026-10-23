-- REJECTED: the action was refused by a business rule (a duplicate phone, an admission number in use, a record not
-- found...) - the system worked, the input was not accepted. Kept apart from FAILURE (an unexpected error) so the
-- platform health check only raises an alert for real faults. See AuditAspect.
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_status_check;
ALTER TABLE audit_logs ADD CONSTRAINT audit_logs_status_check
    CHECK (status IN ('SUCCESS', 'FAILURE', 'REJECTED'));
