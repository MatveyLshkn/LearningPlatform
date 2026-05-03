# Learning Platform Backend

Production-oriented monolithic backend for a learning platform with Keycloak-based authentication, RBAC, Liquibase migrations, and observability.

## What This Application Does
- Manages users, courses, lessons, lectures, enrollments, assessments, and submissions.
- Supports tag-based course discovery.
- Tracks per-user course progress from completed lectures.
- Supports role-based behavior:
  - `ADMIN`
  - `TEACHER`
  - `STUDENT`
- Exposes REST endpoints without `/api` and without URI version segments.
- Uses PostgreSQL + Liquibase for schema evolution.
- Exposes metrics for Prometheus and dashboards in Grafana.

## Tech Stack
- Java 25
- Spring Boot 4.0.4
- Maven
- PostgreSQL 17
- Liquibase
- Keycloak 26
- Prometheus + Grafana + Loki + Promtail
- JSON structured logging

## Project Structure Rules
- Top-level packages only:
  - `by.gsu.learningplatform.core`
  - `by.gsu.learningplatform.capabilities`
- Each feature is a direct package under `capabilities`.
- No artificial `controller/service/repository` subpackages inside a feature.
- Create nested package only for a real subfeature.

## Prerequisites
- JDK 25
- Docker + Docker Compose v2
- Free ports:
  - `5432` (Postgres)
  - `8081` (Keycloak)
  - `9090` (Prometheus)
  - `3100` (Loki)
  - `3000` (Grafana)
  - `8080` (application if run locally)

## Local Run Modes

### 1) Recommended: infra in Docker + app locally
This is the mode you asked for to manually test endpoints.

Start infrastructure:
```bash
docker compose up -d postgres keycloak prometheus loki promtail grafana
```

Run backend locally:
```bash
./mvnw spring-boot:run
```

Stop infrastructure:
```bash
docker compose down
```

### 2) Full stack in Docker (optional)
```bash
docker compose up --build
```

## What Docker Compose Bootstraps
- PostgreSQL databases:
  - `learning_platform` (application)
  - `keycloak` (identity provider)
- Keycloak admin user:
  - username: `admin`
  - password: `admin`
- Predefined platform admin user for API login (`POST /token`):
  - username: `admin`
  - password: `admin`
- Keycloak realm import from:
  - `ops/keycloak/learning-platform-realm.json`
- Imported realm/client:
  - realm: `learning-platform`
  - clientId: `learning-platform-backend`
  - clientSecret: `change-me`
  - realm roles: `ADMIN`, `TEACHER`, `STUDENT`

## Service URLs
- Backend: `http://localhost:8080`
- OpenAPI JSON: `http://localhost:8080/openapi`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Keycloak: `http://localhost:8081`
- Prometheus: `http://localhost:9090`
- Loki: `http://localhost:3100`
- Grafana: `http://localhost:3000` (default login: `admin` / `admin`)

## Manual Endpoint Testing Flow

### 1) Register a teacher
```bash
curl -i -X POST http://localhost:8080/user-registrations \
  -H "Content-Type: application/json" \
  -d '{"username":"teacher1","email":"teacher1@example.com","password":"TeacherPass123!","role":"TEACHER"}'
```

### 2) Get teacher token
```bash
curl -s -X POST \
  http://localhost:8080/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=teacher1&password=TeacherPass123!"
```

Copy `accessToken` from the response.

### 3) Create a course as teacher
```bash
curl -i -X POST http://localhost:8080/courses \
  -H "Authorization: Bearer <teacher_access_token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Java Architecture","description":"Advanced backend course","tags":["java","architecture"]}'
```

### 4) Filter courses by tag
```bash
curl -i "http://localhost:8080/courses?tag=java" \
  -H "Authorization: Bearer <access_token>"
```

### 5) List courses created by current teacher
Use this endpoint for teacher dashboards. The backend resolves the current user from the JWT subject and filters by the local database user id, so the client does not need to compare JWT ids with `teacherId`.
```bash
curl -i "http://localhost:8080/courses/me" \
  -H "Authorization: Bearer <teacher_access_token>"
```

### 6) Register a student and enroll
Repeat registration/token flow for a student (`role":"STUDENT"`), then:
```bash
curl -i -X POST http://localhost:8080/enrollments \
  -H "Authorization: Bearer <student_access_token>" \
  -H "Content-Type: application/json" \
  -d '{"courseId":"<course_id>"}'
```

### 7) Admin user directory
```bash
curl -i "http://localhost:8080/users?role=STUDENT&limit=20" \
  -H "Authorization: Bearer <admin_access_token>"
```

```bash
curl -i "http://localhost:8080/users/<user_id>/details" \
  -H "Authorization: Bearer <admin_access_token>"
```

### 8) View own profile
Any authenticated user can fetch their own basic profile:
```bash
curl -i "http://localhost:8080/users/<own_user_id>" \
  -H "Authorization: Bearer <access_token>"
```

Only admins can fetch another user's profile or use the full learning details endpoint.

### 9) Track course progress
Progress is based on completed lectures in a course.

View your own progress:
```bash
curl -i "http://localhost:8080/courses/<course_id>/progress/me" \
  -H "Authorization: Bearer <access_token>"
```

Mark a lecture completed for a user:
```bash
curl -i -X PUT "http://localhost:8080/courses/<course_id>/users/<user_id>/progress/lectures/<lecture_id>" \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{"completed":true}'
```

View a specific user's course progress:
```bash
curl -i "http://localhost:8080/courses/<course_id>/users/<user_id>/progress" \
  -H "Authorization: Bearer <access_token>"
```

Access rules:
- students can view/update their own progress
- course owner teachers and admins can view/update progress for users in that course
- unrelated users/teachers receive `403`

## Configuration
- Main config file: `src/main/resources/application.yml`
- No multi-environment application YAMLs are used.

### Environment Variables You Must Provide

In current configuration, **all variables below have defaults**.  
So for local development, you can run without specifying env vars.

You need to set variables only when:
- using non-default ports/hosts
- connecting to external Keycloak/Postgres
- overriding AI provider/model settings

### Minimal required set by run mode

1) Infra in Docker + app locally (`./mvnw spring-boot:run`)
- Usually required: none
- If you changed host ports, set:
  - `DB_URL`
  - `KEYCLOAK_ISSUER_URI`
  - `KEYCLOAK_ADMIN_URL`

2) Full stack in Docker (`docker compose up --build`)
- Usually required: none (compose injects app env vars)
- Override only if your environment differs from defaults:
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
  - `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_ADMIN_URL`
  - `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET`

### Full env var reference (with defaults)

- `SERVER_PORT` (default: `8080`)
- `DB_URL` (default: `jdbc:postgresql://localhost:5432/learning_platform`)
- `DB_USERNAME` (default: `learning`)
- `DB_PASSWORD` (default: `learning`)
- `KEYCLOAK_ISSUER_URI` (default: `http://localhost:8081/realms/learning-platform`)
- `KEYCLOAK_ADMIN_URL` (default: `http://localhost:8081`)
- `KEYCLOAK_REALM` (default: `learning-platform`)
- `KEYCLOAK_CLIENT_ID` (default: `learning-platform-backend`)
- `KEYCLOAK_CLIENT_SECRET` (default: `change-me`)
- `KEYCLOAK_ADMIN_USERNAME` (default: `admin`)
- `KEYCLOAK_ADMIN_PASSWORD` (default: `admin`)
- `OLLAMA_BASE_URL` (default: `https://ollama.com`)
- `OLLAMA_API_KEY` (default value exists in config; override strongly recommended outside local/dev)
- `OLLAMA_MODEL` (default: `deepseek-v3.2:cloud`)
- `AI_TEMPERATURE` (default: `0.2`)
- `AI_MAX_TOKENS` (default: `1200`)
- `AI_MAX_PROMPT_CHARS` (default: `12000`)
- `AI_ANALYTICS_RECENT_DAYS` (default: `21`)
- `AI_TIMEOUT_SECONDS` (default: `20`)

Example override (local app mode):
```bash
export DB_URL=jdbc:postgresql://localhost:5433/learning_platform
export KEYCLOAK_ISSUER_URI=http://localhost:8082/realms/learning-platform
export KEYCLOAK_ADMIN_URL=http://localhost:8082
./mvnw spring-boot:run
```

## Database and Migrations
- Liquibase changelogs are under:
  - `src/main/resources/db/changelog`
- Schema changes must be done via Liquibase only.

## Observability
- Metrics endpoint:
  - `GET /actuator/prometheus`
- Prometheus scrapes:
  - `host.docker.internal:8080` (app running locally)
  - `app:8080` (app running in Docker Compose)
- Logs:
  - Docker container logs are collected by Promtail and pushed to Loki.
  - Grafana has two provisioned datasources: `Prometheus` and `Loki`.
  - In Grafana Explore, choose `Loki` and query example: `{composeProject="learningplatform"}`.
- Correlation header:
  - `X-Flow-ID`

## Build and Tests
Compile:
```bash
./mvnw -DskipTests compile
```

Run unit tests:
```bash
./mvnw -Dtest='*UnitTest' test
```

Run full test suite:
```bash
./mvnw test
```
