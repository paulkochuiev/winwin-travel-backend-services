# Testing Guide

## Complete Test Flow

### Step 1: Start All Services

```bash
# Make sure port 8080 is free (stop local application if running)
lsof -ti :8080 | xargs kill -9 2>/dev/null

# Start all services via Docker Compose
docker compose up -d --build

# Check status
docker compose ps
```

### Step 2: Register User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

**Expected result:** HTTP 201 (Created), no response body

### Step 3: Login and Get Token

```bash
# Option 1: Simple request
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Option 2: Save token to variable (for further use)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' \
  | jq -r '.token')

echo "Token: $TOKEN"
```

**Expected result:** HTTP 200

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNzY3ODE5MjgxLCJleHAiOjE3Njc4MjI4ODF9..."
}
```

### Step 4: Process Text (calls data-api and saves log)

```bash
# If token is saved in $TOKEN variable
curl -X POST http://localhost:8080/api/process \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"text":"hello world"}'

# Or with token directly
curl -X POST http://localhost:8080/api/process \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{"text":"hello world"}'
```

**Expected result:** HTTP 200

```json
{ "result": "DLROW OLLEH" }
```

**What happens:**

1. auth-api validates JWT token
2. auth-api calls data-api: `POST http://data-api:8081/api/transform`
3. data-api transforms text (reverse + uppercase)
4. auth-api saves log to database (user_id, input_text, output_text, created_at)
5. auth-api returns result to client

### Step 5: Test data-api Protection

```bash
# Request WITHOUT token - should return 403
curl -X POST http://localhost:8081/api/transform \
  -H "Content-Type: application/json" \
  -d '{"text":"test"}'

# Request with WRONG token - should return 403
curl -X POST http://localhost:8081/api/transform \
  -H "X-Internal-Token: wrong-token" \
  -H "Content-Type: application/json" \
  -d '{"text":"test"}'

# Request with CORRECT token - should return 200
curl -X POST http://localhost:8081/api/transform \
  -H "X-Internal-Token: secret" \
  -H "Content-Type: application/json" \
  -d '{"text":"test"}'
```

**Expected results:**

- Without token: HTTP 403
- Wrong token: HTTP 403
- Correct token: HTTP 200 with `{"result":"TSET"}`

## Viewing Data in PostgreSQL

### Method 1: Via Docker exec (psql)

```bash
# Connect to PostgreSQL container
docker exec -it winwin-postgres psql -U winwin -d winwin

# In psql execute SQL queries:
```

```sql
-- View all users
SELECT id, email, password_hash FROM users;

-- View all processing logs
SELECT
    pl.id,
    pl.user_id,
    u.email,
    pl.input_text,
    pl.output_text,
    pl.created_at
FROM processing_log pl
JOIN users u ON pl.user_id = u.id
ORDER BY pl.created_at DESC;

-- View log count per user
SELECT
    u.email,
    COUNT(pl.id) as log_count
FROM users u
LEFT JOIN processing_log pl ON u.id = pl.user_id
GROUP BY u.email;

-- Exit psql
\q
```

### Method 2: Via Docker exec with single query

```bash
# View all users
docker exec -it winwin-postgres psql -U winwin -d winwin -c "SELECT id, email FROM users;"

# View all logs
docker exec -it winwin-postgres psql -U winwin -d winwin -c "SELECT * FROM processing_log ORDER BY created_at DESC;"

# View logs with user emails
docker exec -it winwin-postgres psql -U winwin -d winwin -c "
SELECT
    pl.id,
    u.email,
    pl.input_text,
    pl.output_text,
    pl.created_at
FROM processing_log pl
JOIN users u ON pl.user_id = u.id
ORDER BY pl.created_at DESC;
"
```

### Method 3: Via External PostgreSQL Client

If you have PostgreSQL client installed (e.g., DBeaver, pgAdmin, or local psql):

```bash
# Connection parameters:
Host: localhost
Port: 5432
Database: winwin
Username: winwin
Password: winwin
```

## Complete Test Script

Create file `test-flow.sh`:

```bash
#!/bin/bash

echo "=== Testing WinWin Travel Backend Services ==="
echo ""

echo "1. Registering user..."
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' \
  -w "\nHTTP Status: %{http_code}\n\n"

echo "2. Login..."
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' \
  | jq -r '.token')

echo "Token received: ${TOKEN:0:50}..."
echo ""

echo "3. Processing text..."
curl -s -X POST http://localhost:8080/api/process \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"text":"hello world"}' \
  -w "\nHTTP Status: %{http_code}\n\n"

echo "4. Testing data-api protection (without token)..."
curl -s -X POST http://localhost:8081/api/transform \
  -H "Content-Type: application/json" \
  -d '{"text":"test"}' \
  -w "\nHTTP Status: %{http_code}\n\n"

echo "5. Testing data-api protection (with correct token)..."
curl -s -X POST http://localhost:8081/api/transform \
  -H "X-Internal-Token: secret" \
  -H "Content-Type: application/json" \
  -d '{"text":"test"}' \
  -w "\nHTTP Status: %{http_code}\n\n"

echo "6. Viewing database data..."
echo "Users:"
docker exec -it winwin-postgres psql -U winwin -d winwin -c "SELECT id, email FROM users;"

echo ""
echo "Processing logs:"
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

echo ""
echo "=== Testing completed ==="
```

Make script executable and run:

```bash
chmod +x test-flow.sh
./test-flow.sh
```

## Checking Docker Container Logs

```bash
# auth-api logs
docker compose logs auth-api --tail 50

# data-api logs
docker compose logs data-api --tail 50

# postgres logs
docker compose logs postgres --tail 50

# All logs
docker compose logs --tail 50
```

## Cleaning Data for Re-testing

```bash
# Clear all data (remove containers and volumes)
docker compose down -v

# Restart
docker compose up -d --build
```
