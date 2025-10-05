CREATE TABLE Users (
    user_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    email NVARCHAR(255) UNIQUE NOT NULL,
    password_hash NVARCHAR(255) NOT NULL,
    subscription_type NVARCHAR(10) DEFAULT 'free' CHECK (subscription_type IN ('free', 'premium')),
    subscription_start_date DATE,
    subscription_end_date DATE,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    last_login DATETIME2 NULL,
    status NVARCHAR(10) DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'deleted')),
);
GO