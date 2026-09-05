-- Platform fee moves from one global row (id = 'platform-fee') to one row per school (id =
-- schools.id) - see PlatformFeeSetting. Carry the previous global amount forward to every
-- existing school first, so nobody's effective platform fee silently changes the moment this
-- migration runs; a school with no row simply has no platform fee configured yet (unchanged
-- behavior - see SeedPlatformFeeForAcademicYearUseCase).
INSERT INTO platform_settings (id, platform_fee_amount, updated_at)
SELECT s.id, gs.platform_fee_amount, now()
FROM schools s
CROSS JOIN (SELECT platform_fee_amount FROM platform_settings WHERE id = 'platform-fee') gs
WHERE gs.platform_fee_amount IS NOT NULL;

DELETE FROM platform_settings WHERE id = 'platform-fee';
