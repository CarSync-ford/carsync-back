ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE customers ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE leads ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE users_aud ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE customers_aud ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE leads_aud ADD COLUMN deleted_at TIMESTAMP;

CREATE INDEX idx_users_deleted_at ON users(deleted_at);
CREATE INDEX idx_customers_deleted_at ON customers(deleted_at);
CREATE INDEX idx_leads_deleted_at ON leads(deleted_at);
