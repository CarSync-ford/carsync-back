ALTER TABLE users ADD COLUMN failed_login_attempts INT DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP;
CREATE INDEX idx_users_locked_until ON users(locked_until);

ALTER TABLE users_aud ADD COLUMN failed_login_attempts INT;
ALTER TABLE users_aud ADD COLUMN locked_until TIMESTAMP;