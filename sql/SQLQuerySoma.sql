USE SomaBD_model;
GO

CREATE TYPE dbo.SubscriptionType FROM VARCHAR(10);
CREATE TYPE dbo.UserStatusType FROM VARCHAR(10);
CREATE TYPE dbo.PlatformType FROM VARCHAR(10);
CREATE TYPE dbo.AuthType FROM VARCHAR(10);
CREATE TYPE dbo.ConnectionStatusType FROM VARCHAR(15);
CREATE TYPE dbo.PaymentStatusType FROM VARCHAR(10);
CREATE TYPE dbo.TransferStatusType FROM VARCHAR(15);
CREATE TYPE dbo.TrackStatusType FROM VARCHAR(10);
CREATE TYPE dbo.ContextType FROM VARCHAR(10);
CREATE TYPE dbo.SeverityType FROM VARCHAR(10);
GO


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

CREATE INDEX idx_users_email ON Users(email);
CREATE INDEX idx_users_subscription ON Users(subscription_type, subscription_end_date);
GO

CREATE TABLE MusicPlatforms (
    platform_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    platform_name NVARCHAR(100) UNIQUE NOT NULL,
    platform_type NVARCHAR(10) NOT NULL CHECK (platform_type IN ('streaming', 'cloud', 'local')),
    api_endpoint_url NVARCHAR(500),
    auth_type NVARCHAR(10) NOT NULL CHECK (auth_type IN ('oauth2', 'api_key', 'basic')),
    is_active BIT DEFAULT 1,
    supported_actions NVARCHAR(MAX),
    rate_limit_per_minute INT DEFAULT 60,
    created_at DATETIME2 DEFAULT GETDATE()
);
GO

CREATE INDEX idx_platforms_active ON MusicPlatforms(is_active);
GO

CREATE TABLE UserPlatformConnections (
    connection_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    platform_id UNIQUEIDENTIFIER NOT NULL,
    external_user_id NVARCHAR(255),
    access_token NVARCHAR(MAX),
    refresh_token NVARCHAR(MAX),
    token_expires_at DATETIME2 NULL,
    connection_status NVARCHAR(15) DEFAULT 'connected' CHECK (connection_status IN ('connected', 'disconnected', 'error')),
    scopes_granted NVARCHAR(MAX),
    last_sync_at DATETIME2 NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (platform_id) REFERENCES MusicPlatforms(platform_id) ON DELETE CASCADE,
    CONSTRAINT unique_user_platform UNIQUE (user_id, platform_id)
);
GO

CREATE INDEX idx_user_connections ON UserPlatformConnections(user_id);
CREATE INDEX idx_platform_connections ON UserPlatformConnections(platform_id);
GO

CREATE TABLE SubscriptionPlans (
    plan_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    plan_name NVARCHAR(100) UNIQUE NOT NULL,
    plan_type NVARCHAR(10) NOT NULL CHECK (plan_type IN ('free', 'premium')),
    monthly_price DECIMAL(10,2) DEFAULT 0.00,
    annual_price DECIMAL(10,2),
    max_platforms INT DEFAULT 2,
    max_monthly_transfers INT DEFAULT 3,
    supports_auto_sync BIT DEFAULT 0,
    supports_advanced_analytics BIT DEFAULT 0,
    priority_processing BIT DEFAULT 0,
    data_export BIT DEFAULT 0,
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE()
);
GO

CREATE INDEX idx_plans_active ON SubscriptionPlans(is_active);
GO

CREATE TABLE Payments (
    payment_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    plan_id UNIQUEIDENTIFIER NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency NVARCHAR(3) DEFAULT 'USD',
    payment_method NVARCHAR(50),
    payment_status NVARCHAR(10) DEFAULT 'pending' CHECK (payment_status IN ('pending', 'completed', 'failed', 'refunded')),
    subscription_start_date DATE,
    subscription_end_date DATE,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES SubscriptionPlans(plan_id)
);
GO

CREATE INDEX idx_payments_user ON Payments(user_id);
CREATE INDEX idx_payments_status ON Payments(payment_status);
GO

CREATE TABLE Tracks (
    track_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    universal_track_id NVARCHAR(100),
    title NVARCHAR(500) NOT NULL,
    artists NVARCHAR(MAX) NOT NULL,
    album NVARCHAR(500),
    duration_ms INT,
    genre NVARCHAR(100),
    release_date DATE,
    popularity_score INT,
    audio_features NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE()
);
GO

CREATE INDEX idx_tracks_universal ON Tracks(universal_track_id);
CREATE INDEX idx_tracks_genre ON Tracks(genre);
GO

CREATE TABLE PlatformTrackMapping (
    mapping_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    track_id UNIQUEIDENTIFIER NOT NULL,
    platform_id UNIQUEIDENTIFIER NOT NULL,
    external_track_id NVARCHAR(255) NOT NULL,
    platform_specific_data NVARCHAR(MAX),
    last_verified_at DATETIME2 NULL,
    is_active BIT DEFAULT 1,
    FOREIGN KEY (track_id) REFERENCES Tracks(track_id) ON DELETE CASCADE,
    FOREIGN KEY (platform_id) REFERENCES MusicPlatforms(platform_id) ON DELETE CASCADE,
    CONSTRAINT unique_track_platform UNIQUE (track_id, platform_id)
);
GO

CREATE INDEX idx_track_mapping ON PlatformTrackMapping(external_track_id, platform_id);
CREATE INDEX idx_mapping_active ON PlatformTrackMapping(is_active);
GO


CREATE TABLE Playlists (
    playlist_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    source_platform_id UNIQUEIDENTIFIER NOT NULL,
    external_playlist_id NVARCHAR(255),
    playlist_name NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX),
    track_count INT DEFAULT 0,
    is_public BIT DEFAULT 0,
    sync_enabled BIT DEFAULT 0,
    last_sync_at DATETIME2 NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (source_platform_id) REFERENCES MusicPlatforms(platform_id)
);
GO

CREATE INDEX idx_playlists_user ON Playlists(user_id);
CREATE INDEX idx_playlists_platform ON Playlists(source_platform_id);
GO

CREATE TABLE PlaylistTracks (
    playlist_track_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    playlist_id UNIQUEIDENTIFIER NOT NULL,
    track_id UNIQUEIDENTIFIER NOT NULL,
    source_platform_id UNIQUEIDENTIFIER NOT NULL,
    position_in_playlist INT,
    added_at DATETIME2 NULL,
    added_by NVARCHAR(255),
    match_confidence DECIMAL(3,2) DEFAULT 1.00,
    FOREIGN KEY (playlist_id) REFERENCES Playlists(playlist_id) ON DELETE CASCADE,
    FOREIGN KEY (track_id) REFERENCES Tracks(track_id) ON DELETE CASCADE,
    FOREIGN KEY (source_platform_id) REFERENCES MusicPlatforms(platform_id),
    CONSTRAINT unique_playlist_track UNIQUE (playlist_id, track_id)
);
GO

CREATE INDEX idx_playlist_tracks ON PlaylistTracks(playlist_id, position_in_playlist);
GO

CREATE TABLE TransferJobs (
    job_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    source_platform_id UNIQUEIDENTIFIER NOT NULL,
    target_platform_id UNIQUEIDENTIFIER NOT NULL,
    status NVARCHAR(15) DEFAULT 'pending' CHECK (status IN ('pending', 'in_progress', 'completed', 'failed', 'cancelled')),
    transfer_type NVARCHAR(10) NOT NULL CHECK (transfer_type IN ('playlist', 'favorites', 'library')),
    source_playlist_id UNIQUEIDENTIFIER,
    target_playlist_name NVARCHAR(255),
    tracks_total INT DEFAULT 0,
    tracks_processed INT DEFAULT 0,
    tracks_successful INT DEFAULT 0,
    tracks_failed INT DEFAULT 0,
    started_at DATETIME2 NULL,
    completed_at DATETIME2 NULL,
    error_message NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (source_platform_id) REFERENCES MusicPlatforms(platform_id),
    FOREIGN KEY (target_platform_id) REFERENCES MusicPlatforms(platform_id),
    FOREIGN KEY (source_playlist_id) REFERENCES Playlists(playlist_id)
);
GO

CREATE INDEX idx_transfer_jobs_user ON TransferJobs(user_id);
CREATE INDEX idx_transfer_jobs_status ON TransferJobs(status, created_at);
GO

CREATE TABLE TransferLogs (
    log_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    job_id UNIQUEIDENTIFIER NOT NULL,
    track_id UNIQUEIDENTIFIER NOT NULL,
    source_track_id NVARCHAR(255),
    target_track_id NVARCHAR(255),
    status NVARCHAR(10) NOT NULL CHECK (status IN ('success', 'failed', 'skipped')),
    match_confidence DECIMAL(3,2),
    failure_reason NVARCHAR(MAX),
    processed_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (job_id) REFERENCES TransferJobs(job_id) ON DELETE CASCADE,
    FOREIGN KEY (track_id) REFERENCES Tracks(track_id) ON DELETE CASCADE
);
GO

CREATE INDEX idx_transfer_logs_job ON TransferLogs(job_id);
CREATE INDEX idx_transfer_logs_status ON TransferLogs(status);
GO

CREATE TABLE ListeningStatistics (
    stat_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    track_id UNIQUEIDENTIFIER NOT NULL,
    platform_id UNIQUEIDENTIFIER NOT NULL,
    listened_at DATETIME2 NOT NULL,
    play_count INT DEFAULT 1,
    play_duration_ms INT,
    context_type NVARCHAR(10) DEFAULT 'playlist' CHECK (context_type IN ('playlist', 'album', 'artist', 'radio')),
    context_id NVARCHAR(255),
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (track_id) REFERENCES Tracks(track_id) ON DELETE CASCADE,
    FOREIGN KEY (platform_id) REFERENCES MusicPlatforms(platform_id)
);
GO

CREATE INDEX idx_listening_stats_user ON ListeningStatistics(user_id, listened_at);
CREATE INDEX idx_listening_stats_track ON ListeningStatistics(track_id);
GO

CREATE TABLE UserStatistics (
    user_stat_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    stat_date DATE NOT NULL,
    total_tracks_listened INT DEFAULT 0,
    total_listening_minutes INT DEFAULT 0,
    unique_artists_count INT DEFAULT 0,
    unique_genres_count INT DEFAULT 0,
    top_artists NVARCHAR(MAX),
    top_genres NVARCHAR(MAX),
    mood_analysis NVARCHAR(MAX),
    discovery_score DECIMAL(4,2),
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    CONSTRAINT unique_user_date UNIQUE (user_id, stat_date)
);
GO

CREATE INDEX idx_user_stats_date ON UserStatistics(stat_date);
GO

CREATE TABLE ErrorLogs (
    error_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NULL,
    error_type NVARCHAR(100) NOT NULL,
    error_message NVARCHAR(MAX) NOT NULL,
    stack_trace NVARCHAR(MAX),
    request_data NVARCHAR(MAX),
    platform_id UNIQUEIDENTIFIER NULL,
    severity NVARCHAR(10) DEFAULT 'medium' CHECK (severity IN ('low', 'medium', 'high', 'critical')),
    resolved BIT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (platform_id) REFERENCES MusicPlatforms(platform_id) ON DELETE SET NULL
);
GO

CREATE INDEX idx_error_logs_severity ON ErrorLogs(severity, created_at);
CREATE INDEX idx_error_logs_resolved ON ErrorLogs(resolved);
GO


CREATE TABLE APIUsageLogs (
    log_id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    platform_id UNIQUEIDENTIFIER NOT NULL,
    endpoint NVARCHAR(255) NOT NULL,
    request_method NVARCHAR(10) NOT NULL,
    response_code INT,
    response_time_ms INT,
    timestamp DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (platform_id) REFERENCES MusicPlatforms(platform_id)
);
GO

CREATE INDEX idx_api_logs_user ON APIUsageLogs(user_id, timestamp);
CREATE INDEX idx_api_logs_platform ON APIUsageLogs(platform_id, timestamp);
GO