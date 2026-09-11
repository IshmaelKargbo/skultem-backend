-- Staff ID cards share the same visual design (colours/layout/dimensions/background) as student
-- cards, but need their own field list (Staff ID, Position, Phone instead of Admission No, Class,
-- Guardian). Nullable - existing rows get backfilled with sensible defaults by
-- GetIdCardSettingUseCase whenever they're read with no staff_fields saved yet, rather than a SQL
-- backfill here.
ALTER TABLE id_card_settings ADD COLUMN staff_fields text;
