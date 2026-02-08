-- DevOS Database Initialization Script

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Set timezone
SET timezone = 'UTC';

-- Create indexes for better performance
-- These will be created automatically by JPA, but we can add additional ones here

-- Full-text search indexes
-- CREATE INDEX CONCURRENTLY idx_projects_search ON projects USING gin(to_tsvector('english', name || ' ' || COALESCE(description, '')));
-- CREATE INDEX CONCURRENTLY idx_ai_messages_search ON ai_messages USING gin(to_tsvector('english', content));

-- JSONB indexes for metadata
-- CREATE INDEX CONCURRENTLY idx_file_nodes_metadata ON file_nodes USING gin(metadata);
-- CREATE INDEX CONCURRENTLY idx_ai_messages_metadata ON ai_messages USING gin(metadata);

-- Composite indexes for common queries
-- CREATE INDEX CONCURRENTLY idx_projects_user_status ON projects(user_id, status);
-- CREATE INDEX CONCURRENTLY idx_ai_messages_project_created ON ai_messages(project_id, created_at DESC);
-- CREATE INDEX CONCURRENTLY idx_audit_logs_project_created ON audit_logs(project_id, created_at DESC);

-- Set up row-level security (optional, for multi-tenant scenarios)
-- ALTER TABLE projects ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE ai_messages ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE file_nodes ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE action_plans ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- Create a function to automatically update updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at (JPA handles this, but as backup)
-- CREATE TRIGGER update_projects_updated_at BEFORE UPDATE ON projects
--     FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
--     FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create a function for soft deletes
CREATE OR REPLACE FUNCTION soft_delete()
RETURNS TRIGGER AS $$
BEGIN
    NEW.status = 'DELETED';
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Insert default admin user (password: admin123)
-- This should be changed in production
INSERT INTO users (username, email, password_hash, first_name, last_name, role, enabled, email_verified, created_at, updated_at)
VALUES (
    'admin',
    'admin@devos.local',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', // password: admin123
    'System',
    'Administrator',
    'ADMIN',
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (username) DO NOTHING;

-- Create a default LLM provider configuration
INSERT INTO llm_providers (name, type, model_name, max_tokens, temperature, is_default, is_active, user_id, created_at)
SELECT 
    'OpenAI GPT-3.5 Turbo',
    'OPENAI',
    'gpt-3.5-turbo',
    4000,
    0.7,
    true,
    true,
    id,
    CURRENT_TIMESTAMP
FROM users 
WHERE username = 'admin'
ON CONFLICT DO NOTHING;

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO devos;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO devos;

-- Set up connection limits for the application user
ALTER USER devos CONNECTION LIMIT 20;

-- Create a view for active projects
CREATE OR REPLACE VIEW active_projects AS
SELECT * FROM projects WHERE status = 'ACTIVE' AND is_indexed = true;

-- Create a view for user statistics
CREATE OR REPLACE VIEW user_stats AS
SELECT 
    u.id,
    u.username,
    u.email,
    COUNT(DISTINCT p.id) as project_count,
    COUNT(DISTINCT CASE WHEN p.status = 'ACTIVE' THEN p.id END) as active_project_count,
    COUNT(DISTINCT m.id) as ai_message_count,
    COALESCE(SUM(m.cost), 0) as total_ai_cost
FROM users u
LEFT JOIN projects p ON u.id = p.user_id AND p.status != 'DELETED'
LEFT JOIN ai_messages m ON p.id = m.project_id
GROUP BY u.id, u.username, u.email;

-- Create a function to clean up old audit logs (optional)
CREATE OR REPLACE FUNCTION cleanup_old_audit_logs(days_to_keep INTEGER DEFAULT 90)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM audit_logs 
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '1 day' * days_to_keep;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Create a function to get project statistics
CREATE OR REPLACE FUNCTION get_project_statistics(project_id_param BIGINT)
RETURNS TABLE(
    total_files BIGINT,
    total_lines BIGINT,
    file_types JSONB,
    last_activity TIMESTAMP,
    ai_interactions BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COUNT(DISTINCT f.id) as total_files,
        COALESCE(SUM(f.line_count), 0) as total_lines,
        jsonb_object_agg(f.language, COUNT(*)::BIGINT) FILTER (WHERE f.language IS NOT NULL) as file_types,
        MAX(a.created_at) as last_activity,
        COUNT(DISTINCT m.id) as ai_interactions
    FROM projects p
    LEFT JOIN file_nodes f ON p.id = f.project_id AND f.type = 'FILE'
    LEFT JOIN audit_logs a ON p.id = a.project_id
    LEFT JOIN ai_messages m ON p.id = m.project_id
    WHERE p.id = project_id_param
    GROUP BY p.id;
END;
$$ LANGUAGE plpgsql;
