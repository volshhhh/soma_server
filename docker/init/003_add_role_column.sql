-- Migration: Add role column to users table
-- Run this if the table already exists without the role column

DO $$
BEGIN
    -- Only add the column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'users' 
        AND column_name = 'role'
    ) THEN
        ALTER TABLE users ADD COLUMN role VARCHAR(255) NOT NULL DEFAULT 'USER';
    END IF;
END $$;

