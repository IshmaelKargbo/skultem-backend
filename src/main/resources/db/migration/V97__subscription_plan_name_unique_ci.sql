-- "Annual" and "annual" are the same plan name.
ALTER TABLE subscription_plans DROP CONSTRAINT subscription_plans_name_key;
CREATE UNIQUE INDEX uq_subscription_plans_name ON subscription_plans (lower(name));
