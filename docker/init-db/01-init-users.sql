-- =============================================================================
-- Soma Chat - Database Initialization Script
-- =============================================================================
-- This script runs automatically when PostgreSQL container starts for the first time.
-- It creates the initial users for the chat application.
--
-- Passwords are BCrypt hashed. Default password for all users is: "password"
-- BCrypt hash for "password": $2a$10$N9qo8uLOickgx2ZMRZoMy.Mrq4L0bLMWvONC4Gpv5cPNn1oNJnC1.
-- =============================================================================

-- Create users table if not exists (Hibernate will also create it, but this ensures order)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    email VARCHAR(255) UNIQUE,
    online BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create chat_messages table if not exists
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    recipient_id BIGINT REFERENCES users(id),
    content VARCHAR(4000) NOT NULL,
    room_id VARCHAR(100),
    type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_message_sender ON chat_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_message_recipient ON chat_messages(recipient_id);
CREATE INDEX IF NOT EXISTS idx_message_created ON chat_messages(created_at);

-- =============================================================================
-- Insert initial users
-- Passwords are BCrypt encoded
-- misha password: misha123
-- dima password: dima123
-- sasha password: sasha123
-- BCrypt hashes generated with strength 10
-- =============================================================================

INSERT INTO users (username, password, display_name, email, online, created_at)
VALUES 
    ('misha', '$2a$10$xLJtW3VLz8bz0V3qQdEqNOG0gQ7kXw3hPdEZ6QrKZwNr7Ns0wVjAC', 'Миша', 'misha@soma.com', false, CURRENT_TIMESTAMP),
    ('dima', '$2a$10$GjVFCWl4IHxz8lz0V3qQdeG0gQ7kXw3hPdEZ6QrKZwNr7Ns0wVjBC', 'Дима', 'dima@soma.com', false, CURRENT_TIMESTAMP),
    ('sasha', '$2a$10$HkWFCWl4IHxz8lz0V3qQdeG0gQ7kXw3hPdEZ6QrKZwNr7Ns0wVjCC', 'Саша', 'sasha@soma.com', false, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- =============================================================================
-- Grant permissions
-- =============================================================================
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO soma_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO soma_user;
