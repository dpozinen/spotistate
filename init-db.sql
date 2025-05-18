-- Spotistate Database Initialization
-- This script runs when the PostgreSQL container starts for the first time

-- Create the database if it doesn't exist (handled by POSTGRES_DB env var)
-- Grant necessary permissions

-- You can add any initial data or schema customizations here
-- The Spring Boot application will handle table creation via JPA

-- Example: Create an index for better performance
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_spotify_id ON users(spotify_id);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_playlists_user_id ON playlists(user_id);
-- CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tracks_playlist_id ON tracks(playlist_id);

-- Log successful initialization
SELECT 'Spotistate database initialized successfully' AS status;
