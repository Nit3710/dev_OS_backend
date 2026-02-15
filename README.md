# DevOS Backend

A comprehensive Spring Boot backend for the DevOS AI-powered developer operating system.

## Overview

DevOS Backend provides a robust API for managing projects, AI-powered code analysis, action plans, and file operations with Git integration. It supports multiple LLM providers and includes real-time features via WebSockets.

## Features

- **Multi-module Architecture**: Clean separation of concerns with core, API, AI integration, and file operations modules
- **AI Integration**: Support for OpenAI, Anthropic, and custom LLM providers with streaming responses
- **Project Management**: Complete CRUD operations with file indexing and search capabilities
- **Git Integration**: Full Git operations including commits, branches, merges, and history
- **Real-time Updates**: WebSocket support for live collaboration and notifications
- **Security**: JWT-based authentication with role-based access control
- **Audit Trail**: Comprehensive logging of all system activities
- **Docker Support**: Containerized deployment with PostgreSQL and Redis

## Architecture

```
backend/
├── devos-core/           # Domain models, repositories, and core services
├── devos-api/            # REST controllers, security, and WebSocket configuration
├── devos-ai-integration/ # AI service implementations and provider factory
├── devos-file-operations/# File system operations and Git integration
└── devos-application/    # Main Spring Boot application
```

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 15+
- Redis 7+
- Docker & Docker Compose (optional)

### Using Docker Compose (Recommended)

1. Clone the repository:
```bash
git clone <repository-url>
cd backend
```

2. Start all services:
```bash
docker-compose up -d
```

3. The application will be available at `http://localhost:8080`
4. API Documentation: `http://localhost:8080/swagger-ui.html`

### Manual Setup

1. Start PostgreSQL and Redis:
```bash
# PostgreSQL
docker run -d --name postgres -e POSTGRES_DB=devos -e POSTGRES_USER=devos -e POSTGRES_PASSWORD=devos123 -p 5432:5432 postgres:15-alpine

# Redis
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

2. Build the application:
```bash
mvn clean package -DskipTests
```

3. Run the application:
```bash
java -jar devos-application/target/devos-application-1.0.0.jar
```

## API Documentation

### Authentication

All API endpoints (except `/auth/login`, `/auth/register`, and `/health`) require JWT authentication.

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

#### Response
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@devos.local",
    "role": "ADMIN"
  }
}
```

### Project Management

#### Get Projects
```http
GET /api/projects?page=0&size=20&sortBy=createdAt&sortDir=desc
Authorization: Bearer <token>
```

#### Create Project
```http
POST /api/projects
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "My Project",
  "description": "A sample project",
  "localPath": "/path/to/project",
  "repositoryUrl": "https://github.com/user/repo.git"
}
```

### AI Integration

#### Send Chat Message
```http
POST /api/ai/chat/{projectId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "Analyze this codebase and suggest improvements",
  "llmProviderId": 1,
  "stream": false
}
```

#### Stream Chat Response
```http
GET /api/ai/chat/{projectId}/stream?message=Your+query&llmProviderId=1
Authorization: Bearer <token>
Accept: text/event-stream
```

### File Operations

#### Get File Content
```http
GET /api/files/{projectId}/content?filePath=src/main/java/Example.java
Authorization: Bearer <token>
```

#### Generate Diff
```http
POST /api/files/{projectId}/diff
Authorization: Bearer <token>
Content-Type: application/json

{
  "filePath": "src/main/java/Example.java",
  "newContent": "// Updated content"
}
```

### Action Plans

#### Get Action Plan
```http
GET /api/ai/plans/{planId}
Authorization: Bearer <token>
```

#### Execute Action Plan
```http
POST /api/ai/plans/{planId}/apply
Authorization: Bearer <token>
```

## WebSocket Support

Connect to WebSocket for real-time updates:

```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // Subscribe to chat messages
    stompClient.subscribe('/user/queue/chat/{projectId}', function(message) {
        console.log('Received:', JSON.parse(message.body));
    });
    
    // Send chat message
    stompClient.send('/app/chat/{projectId}', {}, JSON.stringify({
        content: 'Hello AI',
        threadId: 'optional-thread-id'
    }));
});
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active profile |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/devos` | Database URL |
| `DATABASE_USERNAME` | `devos` | Database username |
| `DATABASE_PASSWORD` | `devos123` | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `JWT_SECRET` | `devos-secret-key` | JWT signing secret |
| `OPENAI_API_KEY` | - | OpenAI API key |
| `ANTHROPIC_API_KEY` | - | Anthropic API key |

### Application Properties

See `application.yml` for complete configuration options.

## Development

### Running Tests

```bash
mvn test
```

### Code Quality

```bash
mvn clean verify
```

### Building for Production

```bash
mvn clean package -Pprod
```

## Monitoring

### Health Checks

- **Application Health**: `GET /api/health`
- **Detailed Health**: `GET /api/health/detailed`
- **Readiness**: `GET /api/health/ready`
- **Liveness**: `GET /api/health/live`

### Metrics

Prometheus metrics are available at `/actuator/prometheus`.

### Logging

Logs are written to `logs/devos.log` and can be configured in `application.yml`.

## Security

### Authentication

- JWT-based authentication with configurable expiration
- Refresh token support
- Role-based access control (ADMIN, DEVELOPER, VIEWER)

### Authorization

- Method-level security using `@PreAuthorize`
- Project-level access control
- API rate limiting (configurable)

## Database Schema

The application uses PostgreSQL with the following main entities:

- **Users**: User accounts and settings
- **Projects**: Project metadata and configuration
- **FileNodes**: File system structure and indexing
- **AIMessages**: AI conversation history
- **ActionPlans**: AI-generated action plans and steps
- **AuditLogs**: System activity tracking

See the database initialization script at `docker/postgres/init.sql` for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For issues and questions, please create an issue in the repository.
