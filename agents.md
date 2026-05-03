# Learning Platform Backend: Agent Context

## Purpose
This project is a production-oriented monolithic backend for an educational platform. It provides REST APIs for managing users, courses, learning content, enrollment, assessments, and submissions, with role-based behavior for `ADMIN`, `TEACHER`, and `STUDENT`.

## Core Domain Capabilities
- Authentication and token exchange via Keycloak (`auth`)
- User registration and user management (`users`)
- Course lifecycle management and tag-based discovery (`courses`)
- Teacher dashboard course listing via `GET /courses/me`, resolved from JWT subject to local user id
- Lessons and lectures content management (`lessons`, `lectures`)
- Student enrollments (`enrollments`)
- Lecture-level course progress tracking (`progress`)
- Assessments and draft/generation flows (`assessments`)
- Submission and grading workflow (`submissions`)
- Admin statistics (`admin`)
- AI insights and study/analytics support (`ai`)

## Technology Stack
### Language and Build
- Java 25
- Maven Wrapper (`./mvnw`)

### Application Framework
- Spring Boot 4.0.4
- Spring Web (REST)
- Spring Data JPA
- Spring Validation
- Spring Security + OAuth2 Resource Server (JWT)
- Spring Boot Actuator

### Data and Persistence
- PostgreSQL 17 (runtime target)
- Liquibase for schema migrations
- Hibernate (JPA)

### Security and Identity
- Keycloak 26 as identity provider
- RBAC with realm roles: `ADMIN`, `TEACHER`, `STUDENT`

### API and Mapping
- springdoc OpenAPI (Swagger UI at `/swagger-ui.html`, spec at `/openapi`)
- MapStruct for DTO/entity mapping

### AI Integration
- Spring AI
- Ollama-compatible chat integration (`spring-ai-ollama`)

### Observability and Operations
- Prometheus metrics endpoint (`/actuator/prometheus`)
- Grafana dashboards
- Loki + Promtail log aggregation
- Structured logging and correlation header (`X-Flow-ID`)
- Docker + Docker Compose for local infrastructure

### Testing
- JUnit 5 via `spring-boot-starter-test`
- Spring Security test utilities
- Testcontainers (PostgreSQL/JDBC)

## High-Level Architecture Notes
- Package roots: `by.gsu.learningplatform.core` and `by.gsu.learningplatform.capabilities`
- Feature-oriented modules under `capabilities/*`
- REST endpoints are exposed without `/api` prefix and without URI version segment
- Configuration is centralized in `src/main/resources/application.yml`

## Current Endpoints (How To Use)
All endpoints are relative to `http://localhost:8080`.  
Authenticated requests require `Authorization: Bearer <access_token>`.

### Auth and Registration
- `POST /user-registrations`
  - Public user signup.
  - Creates a local user + Keycloak identity.
- `POST /token` (`application/x-www-form-urlencoded` or JSON)
  - Exchanges credentials for JWT access token.

### Users
- `GET /users/me`
  - Authenticated.
  - Returns current local user profile resolved from JWT `sub`.
- `GET /users/{userId}`
  - Authenticated.
  - Admin can read anyone; non-admin can read only self.
- `GET /users`
  - Admin only.
  - Query: `limit`, `cursor`, optional `role`.
- `GET /users/{userId}/details`
  - Admin only.
  - Extended user details.

### Courses (Catalog + Management)
- `GET /courses`
  - Public (no auth).
  - Catalog listing with pagination and optional tag filtering.
  - Query: `limit`, `cursor`, `tag` (repeatable).
- `GET /courses/{courseId}`
  - Public (no auth), UUID path only.
  - Catalog details: course metadata + lesson/lecture titles only.
  - Does not expose full lesson/lecture content, assessments, submissions, progress, or AI outputs.
- `GET /courses/me`
  - Teacher/Admin.
  - Courses where current user is owner-teacher.
  - Query: `limit`, `cursor`.
- `GET /courses/enrolled/me`
  - Student/Admin.
  - Courses current user is enrolled in.
  - Returns enrollment-aware payload (`enrollmentId`, `enrolledAt`, `course`).
  - Query: `limit`, `cursor`.
- `POST /courses`
  - Teacher/Admin.
  - Create course.
- `PATCH /courses/{courseId}`
  - Teacher-owner/Admin.
  - Update course metadata.
- `DELETE /courses/{courseId}`
  - Teacher-owner/Admin.
  - Remove course.

### Lessons and Lectures (Protected Content)
- `POST /lessons`
  - Teacher-owner/Admin for target course.
- `GET /lessons/{lessonId}`
  - Admin, course teacher, or enrolled student.
- `GET /courses/{courseId}/lessons`
  - Admin, course teacher, or enrolled student.
- `PATCH /lessons/{lessonId}`
  - Teacher-owner/Admin.
- `DELETE /lessons/{lessonId}`
  - Teacher-owner/Admin.
- `POST /lectures`
  - Teacher-owner/Admin.
  - Requires at least one of `videoUrl` or `content`.
- `GET /lectures/{lectureId}`
  - Admin, course teacher, or enrolled student.
- `GET /lessons/{lessonId}/lectures`
  - Admin, course teacher, or enrolled student.
- `PATCH /lectures/{lectureId}`
  - Teacher-owner/Admin.
- `DELETE /lectures/{lectureId}`
  - Teacher-owner/Admin.

### Enrollment and Course Roster
- `POST /enrollments`
  - Student/Admin.
  - Enroll current user to a course.
- `DELETE /enrollments/{enrollmentId}`
  - Enrollment owner/Admin.
  - Leave course enrollment.
- `GET /enrollments`
  - Authenticated.
  - Current user enrollments.
- `GET /courses/{courseId}/students`
  - Course teacher-owner/Admin.
  - Course roster (students only).

### Assessments
- `POST /assessments`
  - Teacher-owner/Admin.
  - Full creation payload (title/description/questions/answerKey/rubric/source).
- `POST /assessments/generate`
  - Teacher-owner/Admin.
  - AI draft generation.
- `POST /assessments/from-draft`
  - Teacher-owner/Admin.
  - Persists generated draft.
- `GET /courses/{courseId}/assessments`
  - Admin, course teacher, or enrolled student.
  - Student-safe list (no answer keys/rubrics).
- `GET /assessments/{assessmentId}`
  - Admin, course teacher, or enrolled student.
  - Student-safe assessment detail for taking.
- `GET /assessments/{assessmentId}/details`
  - Teacher-owner/Admin.
  - Full assessment including answer key/rubric.

### Submissions and Grading
- `POST /submissions`
  - Student/Admin.
  - Student must be enrolled in assessment’s course.
  - One submission per `(assessmentId, studentId)`; duplicate attempt returns conflict.
- `PATCH /submissions/{submissionId}`
  - Teacher-owner/Admin.
  - Grade submission.
- `GET /submissions/me`
  - Authenticated.
  - Current user submissions.
- `GET /submissions/assessment/{assessmentId}`
  - Teacher-owner/Admin for assessment course.
  - Assessment submission list.

### Progress
- `GET /courses/{courseId}/progress/me`
  - Authenticated user’s own progress in course.
- `GET /courses/{courseId}/users/{userId}/progress`
  - Admin, course teacher, or that user.
  - Student target must be enrolled.
- `PUT /courses/{courseId}/users/{userId}/progress/lectures/{lectureId}`
  - Student only, and only for own `userId`.
  - Lecture must belong to course; student must be enrolled.

### AI Insights
- `GET /courses/{courseId}/ai/student-analytics`
  - Course teacher-owner/Admin.
- `GET /courses/{courseId}/students/{studentId}/ai-study-plan`
  - Course teacher-owner/Admin.
  - `studentId` must be a `STUDENT` enrolled in the course.

### Admin Statistics
- `GET /platform-statistics`
  - Admin only.

## Pagination Contract
- Common query params: `limit`, `cursor`.
- `cursor` is zero-based page number as string (`"0"`, `"1"`, ...).
- Response `page.nextCursor` is `null` when no next page; otherwise next page number string.
