-- The audit log is a record of what happened, so it only ever grows. The database itself refuses to change or
-- remove an entry (and to empty the table), whoever asks - the application has no reason to, and nobody should be
-- able to quietly rewrite history. Entries are added by the audit aspect with plain inserts, which this allows.
CREATE FUNCTION audit_logs_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only: entries cannot be changed or removed'
        USING ERRCODE = 'restrict_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_no_change BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit_logs_append_only();

CREATE TRIGGER audit_logs_no_truncate BEFORE TRUNCATE ON audit_logs
    FOR EACH STATEMENT EXECUTE FUNCTION audit_logs_append_only();
