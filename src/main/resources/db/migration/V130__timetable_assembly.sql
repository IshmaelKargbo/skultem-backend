-- Assembly: a whole-class slot on the timetable, like a break - no subject or teacher, nothing scheduled in it.
ALTER TABLE public.periods DROP CONSTRAINT periods_type_check;
ALTER TABLE public.periods ADD CONSTRAINT periods_type_check
    CHECK (type IN ('PERIOD', 'BREAK', 'LUNCH', 'ASSEMBLY'));
