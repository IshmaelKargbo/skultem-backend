-- supplies.source_sale_id -> material_sales.id and material_sales.supply_id -> supplies.id form a
-- genuine circular reference: creating a sale needs both rows to exist, and each one's foreign key
-- points at the other. No insert order resolves that - whichever row goes first still references a
-- row that doesn't exist yet (V42 tried supply-then-sale, this session's incident tried
-- sale-then-supply; both fail the same way, just on the other constraint). Postgres can check a
-- constraint at COMMIT instead of per-statement if it's declared DEFERRABLE - exactly what a
-- circular reference within one transaction needs. Both sides must be deferrable, since either
-- insert order now needs the check delayed.
ALTER TABLE public.supplies DROP CONSTRAINT supplies_source_sale_id_fkey;
ALTER TABLE public.supplies ADD CONSTRAINT supplies_source_sale_id_fkey
    FOREIGN KEY (source_sale_id) REFERENCES public.material_sales(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE public.material_sales DROP CONSTRAINT material_sales_supply_id_fkey;
ALTER TABLE public.material_sales ADD CONSTRAINT material_sales_supply_id_fkey
    FOREIGN KEY (supply_id) REFERENCES public.supplies(id) DEFERRABLE INITIALLY DEFERRED;
