ALTER TABLE users ADD COLUMN refresh_token_expires_at TIMESTAMP;
ALTER TABLE users_aud ADD COLUMN refresh_token_expires_at TIMESTAMP;