CREATE TABLE user_type (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(255) NOT NULL
);

CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    cpf VARCHAR(255) NOT NULL UNIQUE,
    last_login TIMESTAMP,
    user_type VARCHAR(255) NOT NULL,
    mfa_secret VARCHAR(255),
    mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    refresh_token VARCHAR(255),
    CONSTRAINT fk_users_user_type FOREIGN KEY (user_type) REFERENCES user_type(id)
);

INSERT INTO user_type (id, type) VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'USER');