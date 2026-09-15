ALTER TABLE users ADD COLUMN password_reset_token_hash VARCHAR(64);
ALTER TABLE users ADD COLUMN password_reset_token_expires_at TIMESTAMP;
