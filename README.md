# WinWin Travel Backend Services

Backend services for WinWin Travel, including authentication and data processing APIs built with Spring Boot.

## Architecture

- **Service A (auth-api)**: Spring Boot application with authentication, JWT tokens, and protected endpoints
- **Service B (data-api)**: Spring Boot application for data transformation
- **PostgreSQL**: Database for storing users and processing logs

## Prerequisites

- Docker and Docker Compose
- Maven 3.9+ (for local development)
- Java 17+

## Quick Start

### 1. Build and Run with Docker Compose

```bash
# Build and start all services
docker compose up -d --build

# Check logs
docker compose logs -f

# Stop all services
docker compose down
```

### 2. Manual Build (Alternative)

```bash
# Build auth-api
mvn -f auth-api/pom.xml clean package -DskipTests

# Build data-api
mvn -f data-api/pom.xml clean package -DskipTests

# Then use docker compose
docker compose up -d --build
```

## API Endpoints

### Service A (auth-api) - Port 8080

#### Register User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test123"}'
```

**Response:** HTTP 201 (Created)

#### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"test123"}'
```

**Response:** HTTP 200

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

#### Process Text (Protected)

```bash
curl -X POST http://localhost:8080/api/process \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"text":"hello"}'
```

**Response:** HTTP 200

```json
{ "result": "OLLEH" }
```

### Service B (data-api) - Port 8081

#### Transform Text (Internal Only)

```bash
curl -X POST http://localhost:8081/api/transform \
  -H "X-Internal-Token: secret" \
  -H "Content-Type: application/json" \
  -d '{"text":"hello"}'
```

**Response:** HTTP 200

```json
{ "result": "OLLEH" }
```

**Note:** This endpoint is protected and only accepts requests from Service A with valid `X-Internal-Token` header.

## Complete Test Flow

```bash
# 1. Register a user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'

# 2. Login and save the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  | jq -r '.token')

# 3. Process text (this will call data-api and save a log)
curl -X POST http://localhost:8080/api/process \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"text":"hello world"}'

# Expected response: {"result":"DLROW OLLEH"}
```

## Viewing Database Records

### Quick View via Docker

```bash
# View all users
docker exec -it winwin-postgres psql -U winwin -d winwin -c "SELECT id, email FROM users;"

# View all processing logs with user emails
docker exec -it winwin-postgres psql -U winwin -d winwin -c "
SELECT
    u.email,
    pl.input_text,
    pl.output_text,
    pl.created_at
FROM processing_log pl
JOIN users u ON pl.user_id = u.id
ORDER BY pl.created_at DESC;
"
```

### Interactive PostgreSQL Session

```bash
# Connect to PostgreSQL
docker exec -it winwin-postgres psql -U winwin -d winwin

# Then run SQL queries:
# SELECT * FROM users;
# SELECT * FROM processing_log;
# \q to exit
```

**For detailed testing instructions, see [TESTING.md](TESTING.md)**

## Environment Variables

### auth-api

- `POSTGRES_URL` - PostgreSQL connection URL (default: `jdbc:postgresql://postgres:5432/winwin`)
- `POSTGRES_USER` - PostgreSQL username (default: `winwin`)
- `POSTGRES_PASSWORD` - PostgreSQL password (default: `winwin`)
- `JWT_SECRET` - Secret key for JWT token signing (default: `VERY_LONG_SECRET_KEY_AT_LEAST_32_CHARS_LONG`)
- `DATA_API_URL` - URL of data-api service (default: `http://data-api:8081`)
- `INTERNAL_TOKEN` - Token for internal communication with data-api (default: `secret`)

### data-api

- `INTERNAL_TOKEN` - Token required for requests (default: `secret`)

## Database Schema

### users

- `id` (UUID) - Primary key
- `email` (VARCHAR) - Unique email address
- `password_hash` (VARCHAR) - BCrypt hashed password

### processing_log

- `id` (UUID) - Primary key
- `user_id` (UUID) - Foreign key to users
- `input_text` (VARCHAR) - Original input text
- `output_text` (VARCHAR) - Transformed output text
- `created_at` (TIMESTAMP) - Creation timestamp

## Project Structure

```
.
├── auth-api/              # Service A - Authentication API
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/winwin/travel/authapi/
│   │       │       ├── controller/    # REST controllers
│   │       │       ├── service/       # Business logic
│   │       │       ├── entity/        # JPA entities
│   │       │       ├── repository/    # Data repositories
│   │       │       ├── security/      # Security configuration
│   │       │       └── dto/           # Data transfer objects
│   │       └── resources/
│   │           └── application.yml
│   └── Dockerfile
├── data-api/              # Service B - Data Processing API
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/winwin/travel/dataapi/
│   │       │       └── controller/
│   │       └── resources/
│   │           └── application.yaml
│   └── Dockerfile
├── docker-compose.yml     # Docker Compose configuration
└── README.md             # This file
```

## Development

### Running Locally (without Docker)

1. Start PostgreSQL:

```bash
docker compose up -d postgres
```

2. Run auth-api:

```bash
cd auth-api
./mvnw spring-boot:run
```

3. Run data-api (in another terminal):

```bash
cd data-api
./mvnw spring-boot:run
```

### Testing

All endpoints can be tested using curl commands provided above. The complete flow:

1. Register a user
2. Login to get JWT token
3. Use token to access protected `/api/process` endpoint
4. The process endpoint calls data-api and saves a log entry

## Notes

- Passwords are hashed using BCrypt
- JWT tokens expire after 1 hour (3600000 ms)
- Service B only accepts requests with valid `X-Internal-Token` header
- All processing requests are logged in the database
