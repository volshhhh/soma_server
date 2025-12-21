-- Migration: Add email verification columns to users table
-- Run this if the table already exists

DO $$
BEGIN
    -- Add email_verified column
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users'
        AND column_name = 'email_verified'
    ) THEN
        ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;

    -- Add verification_token column
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users'
        AND column_name = 'verification_token'
    ) THEN
        ALTER TABLE users ADD COLUMN verification_token VARCHAR(64);
    END IF;

    -- Add verification_token_expiry column
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users'
        AND column_name = 'verification_token_expiry'
    ) THEN
        ALTER TABLE users ADD COLUMN verification_token_expiry TIMESTAMP;
    END IF;
END $$;

-- Create index for verification token lookup
CREATE INDEX IF NOT EXISTS idx_users_verification_token ON users(verification_token);

