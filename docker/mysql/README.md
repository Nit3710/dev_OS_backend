# MySQL Database Configuration

This directory contains MySQL configuration for the DevOS application.

## Database Details

- **Database Name**: devos
- **User**: devos
- **Password**: devos123
- **Root Password**: root123
- **Port**: 3306
- **Character Set**: utf8mb4
- **Collation**: utf8mb4_unicode_ci

## Connection URL

```
jdbc:mysql://localhost:3306/devos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

## Docker Configuration

The MySQL container is configured in `docker-compose.yml` with the following settings:

- MySQL 8.0 image
- Persistent data volume
- Health checks
- Native password authentication
- Automatic database initialization

## Environment Variables

- `MYSQL_DATABASE`: devos
- `MYSQL_USER`: devos
- `MYSQL_PASSWORD`: devos123
- `MYSQL_ROOT_PASSWORD`: root123
- `MYSQL_CHARACTER_SET_SERVER`: utf8mb4
- `MYSQL_COLLATION_SERVER`: utf8mb4_unicode_ci

## Migration from PostgreSQL

The application has been migrated from PostgreSQL to MySQL. Key changes:

1. **JDBC Driver**: Changed from PostgreSQL to MySQL Connector/J
2. **Hibernate Dialect**: Changed to `MySQLDialect`
3. **Connection URL**: Updated to MySQL format with required parameters
4. **Docker Service**: Replaced PostgreSQL container with MySQL
5. **Testcontainers**: Updated to use MySQL for integration tests

## Notes

- MySQL uses `mysql_native_password` authentication for compatibility
- SSL is disabled for local development
- Timezone is set to UTC to avoid timezone-related issues
- The database will be automatically created by Hibernate with `ddl-auto: update`
