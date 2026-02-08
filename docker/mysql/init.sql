-- DevOS MySQL Database Initialization Script

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS devos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the devos database
USE devos;

-- Create user and grant privileges (if not already created by MySQL environment variables)
-- This is handled by MySQL environment variables in docker-compose.yml

-- Set default character set
ALTER DATABASE devos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Additional initialization can be added here
-- For example, creating initial tables or inserting default data

-- Note: Hibernate will handle table creation with ddl-auto: update
-- This script is mainly for database-level configuration
