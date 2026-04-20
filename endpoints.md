# API Endpoint Test Documentation

## Test Context
- Run date: 2026-04-20
- Runtime target: Docker Compose (`http://localhost:8080`)
- Scope: business endpoints only (no `/actuator/*`, `/openapi`, `/swagger-ui*`)
- Role set used: `ADMIN`, `TEACHER`, `STUDENT`
- Evidence artifacts: `/tmp/lp-api-artifacts-v4-1776711786`

## Docker Compose Notes
- Updated `docker-compose.yml` so `app` starts in default `docker compose up` flow (removed `fullstack` profile from `app`).
- Added Keycloak healthcheck and changed app dependency to `condition: service_healthy`.

## Auth Note
- `POST /token` is now verified with `application/x-www-form-urlencoded` and JSON request handling in the API.
- Endpoint tests in this document use `/token` responses directly for bearer authentication.

## Common Error Shape
Most domain/business errors return:
```json
{
  "type": "https://learning-platform/errors/...",
  "title": "...",
  "status": 400,
  "detail": "...",
  "instance": "/...",
  "correlationId": "...",
  "timestamp": "..."
}
```
Some security-layer `401/403` responses were empty body in this environment.

## Test Data Lifecycle
- Registered users: `teachera_1776711364`, `teacherb_1776711364`, `studenta_1776711364`, `studentb_1776711364`.
- Primary course: `40af318e-0fdf-4d33-ad7d-182778d189ed`.
- Lesson: `fa358598-5259-40b6-bb28-30892b00fe34`.
- Lecture: `90c97295-da01-4be2-aca0-ccb671e0f1a6`.
- Assessment: `c99a0cb8-c3a9-4b68-bf79-2cfbcaba03f7`.
- Draft-created assessment: `ee1c7303-ff02-4e00-9df7-85cf00ee5085`.
- Submission: `0fdd337a-d685-43f6-bdf4-875f60c7147c`.
- Enrollment flow used create/delete/recreate to verify owner vs non-owner/admin behavior.

---

## `POST /user-registrations`
- Auth: public
- Params: none
- Request body:
  - `username` string, required, 3..64
  - `email` string, required, valid email
  - `password` string, required, 8..100
  - `role` enum required: `TEACHER|STUDENT` (`ADMIN` rejected)
- Example request:
```json
{"username":"teachera_1776711364","email":"teachera_1776711364@example.com","password":"Pass12345!","role":"TEACHER"}
```
- Success `201`:
```json
{"userId":"760e8367-510d-4928-8145-bba08e6a5462","keycloakSub":"a685d2fd-9857-482d-b3a9-1323b1b5ebb2","username":"teachera_1776711364","email":"teachera_1776711364@example.com","role":"TEACHER"}
```
- Key error cases:
  - `400` (`ADMIN cannot be self-assigned`)
  - `409` (`Username already exists`)
  - `422` validation errors
- Verified statuses: `201, 400, 409, 422`

## `POST /token`
- Auth: public
- Params: none
- Request body (supported formats):
  - JSON: `{"username":"...","password":"..."}`
  - Form URL encoded: `username=...&password=...`
- Example request:
```text
username=admin&password=admin
```
- Success response `200`:
```json
{"accessToken":"<jwt>","tokenType":"Bearer","expiresIn":300,"refreshToken":"<jwt>","refreshExpiresIn":1800,"scope":"email profile"}
```
- Error response `400` (invalid credentials):
```json
{"type":"https://learning-platform/errors/bad-request","title":"Bad Request","status":400,"detail":"Invalid username or password","instance":"/token","correlationId":"...","timestamp":"..."}
```
- Verified statuses: `200, 400`

## `GET /users/{userId}`
- Auth: required
- Roles: `ADMIN`, `TEACHER` (student forbidden)
- Path params:
  - `userId` UUID required
- Success `200`:
```json
{"id":"8b0e4a85-21c4-4a98-9637-1156bd3dd2f9","username":"studenta_1776711364","email":"studenta_1776711364@example.com","role":"STUDENT","createdAt":"2026-04-20T18:56:05.234477Z"}
```
- Errors:
  - `403` for student token
  - `401` missing token
  - `404` unknown `userId`
- Verified statuses: `200, 403, 401, 404`

## `POST /courses`
- Auth: required
- Roles: `TEACHER`, `ADMIN`
- Request body:
  - `title` string required max 255
  - `description` string optional max 5000
- Success `201`:
```json
{"id":"40af318e-0fdf-4d33-ad7d-182778d189ed","title":"Java Architecture 1776711364","description":"Advanced backend course","teacherId":"760e8367-510d-4928-8145-bba08e6a5462","createdAt":null}
```
- Errors:
  - `403` for student token
- Verified statuses: `201, 403`

## `GET /courses`
- Auth: required
- Roles: any authenticated
- Query params:
  - `limit` integer optional
  - `cursor` string optional
- Success `200` example:
```json
{"items":[{"id":"36054c5c-9d73-40cd-b627-81f426fe815b","title":"AI Tested Course","description":"Rich backend testing course for AI features.","teacherId":"024c9041-170b-48ad-853f-b93c1c8957d6","createdAt":"2026-04-06T15:40:29.501424Z"}],"page":{"limit":5,"returned":5,"nextCursor":"demo-cursor"},"links":{"self":"/courses"}}
```
- Errors:
  - `401` unauthenticated
- Verified statuses: `200, 401`

## `PATCH /courses/{courseId}`
- Auth: required
- Roles: `TEACHER`, `ADMIN`
- Path params:
  - `courseId` UUID required
- Request body: same as `POST /courses`
- Success `200` (owner/admin)
- Errors:
  - `403` non-owner teacher
- Verified statuses: `200, 403`

## `DELETE /courses/{courseId}`
- Auth: required
- Roles: `TEACHER`, `ADMIN`
- Path params:
  - `courseId` UUID required
- Success: `204`
- Errors:
  - `403` non-owner teacher
- Verified statuses: `204, 403`

## `POST /lessons`
- Auth: required
- Roles: security allows `TEACHER|ADMIN`; service enforces course owner/admin
- Request body:
  - `courseId` UUID required
  - `title` string required max 255
  - `content` string required
- Success `201`:
```json
{"id":"fa358598-5259-40b6-bb28-30892b00fe34","courseId":"40af318e-0fdf-4d33-ad7d-182778d189ed","title":"Lesson 1 1776711364","content":"Lesson content"}
```
- Errors:
  - `403` non-owner teacher
  - `403` student
- Verified statuses: `201, 403`

## `GET /courses/{courseId}/lessons`
- Auth: required
- Roles: any authenticated
- Path params:
  - `courseId` UUID required
- Success `200`:
```json
[{"id":"fa358598-5259-40b6-bb28-30892b00fe34","courseId":"40af318e-0fdf-4d33-ad7d-182778d189ed","title":"Lesson 1 1776711364","content":"Lesson content"}]
```
- Verified statuses: `200`

## `POST /lectures`
- Auth: required
- Roles: security allows `TEACHER|ADMIN`; service enforces course owner/admin
- Request body:
  - `lessonId` UUID required
  - `title` string required max 255
  - `videoUrl` string optional
  - `content` string optional
  - constraint: at least one of `videoUrl` or `content` must be non-blank
- Success `201`:
```json
{"id":"90c97295-da01-4be2-aca0-ccb671e0f1a6","lessonId":"fa358598-5259-40b6-bb28-30892b00fe34","title":"Lecture 1 1776711364","videoUrl":"https://example.com/v.mp4","content":"Lecture content"}
```
- Errors:
  - `400` when `videoUrl` and `content` are blank
  - `403` non-owner teacher
- Verified statuses: `201, 400, 403`

## `GET /lessons/{lessonId}/lectures`
- Auth: required
- Roles: any authenticated
- Path params:
  - `lessonId` UUID required
- Success `200`:
```json
[{"id":"90c97295-da01-4be2-aca0-ccb671e0f1a6","lessonId":"fa358598-5259-40b6-bb28-30892b00fe34","title":"Lecture 1 1776711364","videoUrl":"https://example.com/v.mp4","content":"Lecture content"}]
```
- Verified statuses: `200`

## `POST /enrollments`
- Auth: required
- Roles: security allows `STUDENT|ADMIN`; service forbids `TEACHER`
- Request body:
  - `courseId` UUID required
- Success `201`:
```json
{"id":"7c019a9d-69ee-48a1-bcc0-fd0da393c537","userId":"8b0e4a85-21c4-4a98-9637-1156bd3dd2f9","courseId":"40af318e-0fdf-4d33-ad7d-182778d189ed","enrolledAt":null}
```
- Errors:
  - `403` teacher
  - `409` duplicate enrollment
- Verified statuses: `201, 403, 409`

## `DELETE /enrollments/{enrollmentId}`
- Auth: required
- Roles: `STUDENT|ADMIN`
- Path params:
  - `enrollmentId` UUID required
- Success: `204` (owner student or admin)
- Errors:
  - `403` other student/non-owner
- Verified statuses: `204, 403`

## `GET /enrollments`
- Auth: required
- Roles: any authenticated
- Params: none
- Behavior: returns own enrollments
- Success `200` example:
```json
[]
```
- Verified statuses: `200`

## `POST /assessments`
- Auth: required
- Roles: `TEACHER|ADMIN` with course-owner/admin check
- Request body:
  - `courseId` UUID required
- Success `201`:
```json
{"id":"c99a0cb8-c3a9-4b68-bf79-2cfbcaba03f7","courseId":"40af318e-0fdf-4d33-ad7d-182778d189ed","createdBy":"760e8367-510d-4928-8145-bba08e6a5462","title":null,"description":null,"questionsJson":null,"answerKeyJson":null,"rubricJson":null,"sourceType":null,"sourceId":null,"createdAt":null}
```
- Errors:
  - `403` non-owner teacher
  - `403` student
- Verified statuses: `201, 403`

## `POST /assessments/generate`
- Auth: required
- Roles: `TEACHER|ADMIN` with course-owner/admin check
- Request body:
  - `courseId` UUID required
  - `lessonId` UUID optional
  - `lectureId` UUID optional
  - `questionCount` integer optional 3..20
  - `difficulty` string optional max 32
  - constraint: not both `lessonId` and `lectureId`
- Success `200`: returns draft payload with `title`, `description`, `questions[]`, `answerKey[]`, `rubricCriteria[]`
- Error `400`:
```json
{"type":"https://learning-platform/errors/bad-request","title":"Bad Request","status":400,"detail":"Use lessonId or lectureId, not both","instance":"/assessments/generate","correlationId":"9b3e4582-898d-4759-8aeb-c300deedad2e","timestamp":"2026-04-20T18:56:21.738855163Z"}
```
- Verified statuses: `200, 400`

## `POST /assessments/from-draft`
- Auth: required
- Roles: `TEACHER|ADMIN` with course-owner/admin check
- Request body:
  - `courseId` UUID required
  - `lessonId` UUID optional
  - `lectureId` UUID optional
  - `title` string required max 255
  - `description` string required max 5000
  - `questions` non-empty string[]
  - `answerKey` non-empty string[]
  - `rubricCriteria` non-empty string[]
  - constraint: not both `lessonId` and `lectureId`
- Success `201`:
```json
{"id":"ee1c7303-ff02-4e00-9df7-85cf00ee5085","courseId":"40af318e-0fdf-4d33-ad7d-182778d189ed","createdBy":"760e8367-510d-4928-8145-bba08e6a5462","title":"Draft Assessment 1776711364","description":"Assessment from draft","questionsJson":"[\"Q1\",\"Q2\"]","answerKeyJson":"[\"A1\",\"A2\"]","rubricJson":"[\"R1\",\"R2\"]","sourceType":"LESSON","sourceId":"fa358598-5259-40b6-bb28-30892b00fe34","createdAt":null}
```
- Error:
  - `400` when both `lessonId` and `lectureId` provided
- Verified statuses: `201, 400`

## `GET /courses/{courseId}/assessments`
- Auth: required
- Roles: any authenticated
- Path params:
  - `courseId` UUID required
- Success `200`: `AssessmentResponse[]`
- Verified statuses: `200`

## `POST /submissions`
- Auth: required
- Roles: security allows `STUDENT|ADMIN`; service forbids `TEACHER`
- Request body:
  - `assessmentId` UUID required
  - `answerText` string required
- Success `201`:
```json
{"id":"0fdd337a-d685-43f6-bdf4-875f60c7147c","assessmentId":"c99a0cb8-c3a9-4b68-bf79-2cfbcaba03f7","studentId":"8b0e4a85-21c4-4a98-9637-1156bd3dd2f9","answerText":"My answer","score":null,"submittedAt":null,"gradedAt":null}
```
- Errors:
  - `403` teacher
- Verified statuses: `201, 403`

## `PATCH /submissions/{submissionId}`
- Auth: required
- Roles: `TEACHER|ADMIN` with course-owner/admin check
- Path params:
  - `submissionId` UUID required
- Request body:
  - `score` integer required 0..100
- Success `200`:
```json
{"id":"0fdd337a-d685-43f6-bdf4-875f60c7147c","assessmentId":"c99a0cb8-c3a9-4b68-bf79-2cfbcaba03f7","studentId":"8b0e4a85-21c4-4a98-9637-1156bd3dd2f9","answerText":"My answer","score":85,"submittedAt":"2026-04-20T18:56:21.844423Z","gradedAt":"2026-04-20T18:56:21.902607548Z"}
```
- Errors:
  - `403` non-owner teacher
  - `403` student
- Verified statuses: `200, 403`

## `GET /submissions/me`
- Auth: required
- Roles: any authenticated
- Params: none
- Success `200`:
```json
[{"id":"0fdd337a-d685-43f6-bdf4-875f60c7147c","assessmentId":"c99a0cb8-c3a9-4b68-bf79-2cfbcaba03f7","studentId":"8b0e4a85-21c4-4a98-9637-1156bd3dd2f9","answerText":"My answer","score":85,"submittedAt":"2026-04-20T18:56:21.844423Z","gradedAt":"2026-04-20T18:56:21.902608Z"}]
```
- Verified statuses: `200`

## `GET /submissions/assessment/{assessmentId}`
- Auth: required
- Roles: any authenticated
- Path params:
  - `assessmentId` UUID required
- Success `200`: `SubmissionResponse[]`
- Verified statuses: `200`

## `GET /platform-statistics`
- Auth: required
- Roles: `ADMIN`
- Params: none
- Success `200`:
```json
{"usersCount":60,"coursesCount":15,"enrollmentsCount":32,"averageSubmissionScore":73.3225806451613}
```
- Errors:
  - `403` teacher
  - `403` student
- Verified statuses: `200, 403`

## `GET /courses/{courseId}/ai/student-analytics`
- Auth: required
- Roles: `TEACHER|ADMIN` with course-owner/admin check
- Path params:
  - `courseId` UUID required
- Success `200` response fields:
  - `courseId`, `averageScore`, `totalSubmissions`, `gradedSubmissions`, `recencyTrendDelta`, `aiSummary`, `students[]`
- Error:
  - `403` non-owner teacher/student
- Verified statuses: `200, 403`

## `GET /courses/{courseId}/students/{studentId}/ai-study-plan`
- Auth: required
- Roles: `TEACHER|ADMIN` with course-owner/admin check
- Path params:
  - `courseId` UUID required
  - `studentId` UUID required
- Success `200`:
```json
{"courseId":"40af318e-0fdf-4d33-ad7d-182778d189ed","studentId":"8b0e4a85-21c4-4a98-9637-1156bd3dd2f9","prioritizedGoals":["Master advanced Java architecture patterns"],"weeklyTargets":["Complete 3 advanced architecture pattern implementations with 90% accuracy"],"recommendedLessons":["Lesson Admin 1776711364","Lesson 1 1776711364"],"recommendedLectures":["Lecture 1 1776711364"],"rationale":"The student shows solid foundational knowledge with an 85% average score and stable performance trend. ..."}
```
- Errors:
  - `403` non-owner teacher/student
- Verified statuses: `200, 403`

---

## Verified Status Index
Source: `/tmp/lp-api-artifacts-v4-1776711786/statuses.tsv` (81 API calls)
- Every business endpoint in scope was invoked at least once.
- Role-based positive and negative cases were executed for protected endpoints.
- `/token` verified with successful and invalid-credential flows.
