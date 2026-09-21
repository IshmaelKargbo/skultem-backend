-- Some databases (production) still carry the old single-master column houses.house_master_id,
-- NOT NULL, from before a house could have several masters. HouseEntity now stores masters in
-- houses_house_masters and never writes that column, so every INSERT INTO houses fails with a
-- not-null violation. Fresh databases (V1 baseline) never had it, hence the guard.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'houses' AND column_name = 'house_master_id'
    ) THEN
        CREATE TABLE IF NOT EXISTS public.houses_house_masters (
            house_entity_id character varying(255) NOT NULL,
            house_masters_id character varying(255) NOT NULL
        );

        -- Keep each existing house's master by moving it into the join table.
        INSERT INTO public.houses_house_masters (house_entity_id, house_masters_id)
        SELECT h.id, h.house_master_id
        FROM public.houses h
        WHERE h.house_master_id IS NOT NULL
          AND NOT EXISTS (
              SELECT 1 FROM public.houses_house_masters x
              WHERE x.house_entity_id = h.id AND x.house_masters_id = h.house_master_id
          );

        ALTER TABLE public.houses DROP COLUMN house_master_id;
    END IF;
END $$;
