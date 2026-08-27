-- The working_days.state column was created as varchar by an earlier Hibernate
-- ddl-auto=update run, but the WorkingDayEntity.state field is a boolean.
-- Hibernate's schema validation only checks for drift, it never corrects it,
-- so this went unnoticed until Flyway took over and validate mode caught it.
ALTER TABLE public.working_days
    ALTER COLUMN state TYPE boolean USING (state::boolean);
