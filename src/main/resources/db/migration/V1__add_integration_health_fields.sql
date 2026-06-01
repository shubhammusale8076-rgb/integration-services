-- Optional manual migration when not using spring.jpa.hibernate.ddl-auto=update
ALTER TABLE tenant_integrations ADD COLUMN IF NOT EXISTS health_status VARCHAR(32);
ALTER TABLE tenant_integrations ADD COLUMN IF NOT EXISTS last_validated_at TIMESTAMP;
ALTER TABLE tenant_integrations ADD COLUMN IF NOT EXISTS last_health_check_at TIMESTAMP;
ALTER TABLE tenant_integrations ADD COLUMN IF NOT EXISTS last_error TEXT;
ALTER TABLE tenant_integrations ADD COLUMN IF NOT EXISTS reauth_required BOOLEAN DEFAULT FALSE;
ALTER TABLE tenant_integrations ADD COLUMN IF NOT EXISTS consecutive_failures INTEGER DEFAULT 0;

UPDATE tenant_integrations SET health_status = 'CONNECTED' WHERE health_status IS NULL;
UPDATE tenant_integrations SET consecutive_failures = 0 WHERE consecutive_failures IS NULL;
UPDATE tenant_integrations SET reauth_required = FALSE WHERE reauth_required IS NULL;
