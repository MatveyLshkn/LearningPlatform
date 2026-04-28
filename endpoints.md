# API Endpoint Test Documentation

## Test Context
- Run date: 2026-04-28
- Runtime target: Docker Compose (`http://localhost:8080`)
- Scope: business endpoints only (no `/actuator/*`, `/openapi`, `/swagger-ui*`)
- Role set used: `ADMIN`, `TEACHER`, `STUDENT`
- Evidence artifacts:
  - Status matrix: `/tmp/manual_api_results_manual1777311128.tsv`
  - Response bodies: `/tmp/manual_api_bodies_manual1777311128`
  - Admin enrollment cleanup checks: `/tmp/manual_api_admin_enrollment_cleanup_manual1777311128.json`
  - Self-profile access check: `/tmp/self_profile_self1777406396`
  - Course progress flow: `/tmp/progress_flow_progress1777406834`
  - Authored course listing flow: `/tmp/courses_me_mine1777407095`
  - Current full endpoint flow: `/tmp/full_api_flow_full1777407269.tsv`
  - Current full endpoint response bodies: `/tmp/full_api_flow_full1777407269`

## Docker Compose Notes
- `docker compose up --build` starts the app on `http://localhost:8080`.
- The app service explicitly sets `SERVER_PORT=8080`, matching the published `8080:8080` port mapping.
- Keycloak is exposed on `http://localhost:8081` and imported realm/client defaults are used.

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
- Registered users: `teacher_a_manual1777311128`, `teacher_b_manual1777311128`, `student_a_manual1777311128`, `student_b_manual1777311128`.
- Primary course: `9b5657d9-c323-4a65-b433-215c06e25929`.
- Lesson: `cf292aa2-f31f-4841-bac8-37769c2f94a1`.
- Lecture: `2c1743a0-66a8-4d6d-b0dc-cd3fefc430b4`.
- Assessment: `ec22045c-a45b-42b7-a63a-c5b59b846847`.
- Submission: `5bcb727d-8b0d-461f-86ca-ccd1bc695248`.
- Enrollment flow used create/delete/recreate to verify owner vs non-owner/admin behavior.
- The first assessment generation request intentionally exposed a validation rule: `questionCount=2` returns `422` because the allowed range is `3..20`. The corrected `questionCount=3` request returned `200`.
- Self-profile flow verified `GET /users/{userId}` returns `200` for teacher/student reading their own profile, `403` for non-owner access, and `200` for admin access.
- Course progress flow verified student self-read/update, course-teacher read/update, unrelated teacher denial, and recalculation after marking a lecture incomplete.
- Authored course listing flow verified each teacher sees only courses they created, and students receive `403`.
- Current full endpoint flow verified 70 manual curl calls with `PASS=70 FAIL=0`.

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
- Roles: `ADMIN`, or the user who owns `{userId}`
- Path params:
  - `userId` UUID required
- Success `200`:
```json
{"id":"8b0e4a85-21c4-4a98-9637-1156bd3dd2f9","username":"studenta_1776711364","email":"studenta_1776711364@example.com","role":"STUDENT","createdAt":"2026-04-20T18:56:05.234477Z"}
```
- Errors:
  - `403` for authenticated non-owner non-admin token
  - `401` missing token
  - `404` unknown `userId`
- Manual self-profile verification:
  - teacher own profile: `200`
  - student own profile: `200`
  - student reading teacher profile: `403`
  - admin reading student profile: `200`
- Verified statuses: `200, 403, 401, 404`

## `GET /users`
- Auth: required
- Roles: `ADMIN`
- Query params:
  - `limit` integer optional
  - `cursor` string optional
  - `role` enum optional: `ADMIN|TEACHER|STUDENT`
- Success `200`:
```json
{"items":[{"id":"168b020c-3ef9-4c3c-a027-330eef32761b","username":"student1","email":"student1@example.com","role":"STUDENT","createdAt":"2026-04-27T17:30:00Z"}],"page":{"limit":5,"returned":1,"nextCursor":null},"links":{"self":"/users"}}
```
- Errors:
  - `401` unauthenticated
  - `403` teacher/student
- Verified statuses: `200, 401, 403`

## `GET /users/{userId}/details`
- Auth: required
- Roles: `ADMIN`
- Path params:
  - `userId` UUID required
- Success `200`: includes `user`, `enrollments`, `enrolledCourses`, `taughtCourses`, `submissions`, and `createdAssessments`.
```json
{"user":{"id":"168b020c-3ef9-4c3c-a027-330eef32761b","username":"student1","email":"student1@example.com","role":"STUDENT","createdAt":"2026-04-27T17:30:00Z"},"enrollments":[],"enrolledCourses":[],"taughtCourses":[],"submissions":[],"createdAssessments":[]}
```
- Errors:
  - `401` unauthenticated
  - `403` teacher/student
  - `404` unknown `userId`
- Verified statuses: `200, 401, 403, 404`

## `POST /courses`
- Auth: required
- Roles: `TEACHER`, `ADMIN`
- Request body:
  - `title` string required max 255
  - `description` string optional max 5000
  - `tags` string array optional, max 20 tags, each max 64 chars
- Success `201`:
```json
{"id":"40af318e-0fdf-4d33-ad7d-182778d189ed","title":"Java Architecture 1776711364","description":"Advanced backend course","teacherId":"760e8367-510d-4928-8145-bba08e6a5462","createdAt":null,"tags":["java","spring"]}
```
- Notes: tags are trimmed, lowercased, deduplicated, and created automatically.
- Errors:
  - `403` for student token
- Verified statuses: `201, 403`

## `GET /courses`
- Auth: required
- Roles: any authenticated
- Query params:
  - `limit` integer optional
  - `cursor` string optional
  - `tag` string optional, repeatable; multiple values use OR semantics
- Success `200` example:
```json
{"items":[{"id":"36054c5c-9d73-40cd-b627-81f426fe815b","title":"AI Tested Course","description":"Rich backend testing course for AI features.","teacherId":"024c9041-170b-48ad-853f-b93c1c8957d6","createdAt":"2026-04-06T15:40:29.501424Z","tags":["java"]}],"page":{"limit":5,"returned":5,"nextCursor":"demo-cursor"},"links":{"self":"/courses"}}
```
- Errors:
  - `401` unauthenticated
- Verified statuses: `200, 401`

## `GET /courses/me`
- Auth: required
- Roles: `TEACHER|ADMIN`
- Query params:
  - `limit` integer optional
  - `cursor` string optional
- Behavior: returns courses authored by the current authenticated local user. The backend resolves the user from JWT `sub` and filters by the database `users.id`, avoiding client-side comparison between Keycloak subject ids and local `teacherId` UUIDs.
- Success `200` example:
```json
{"items":[{"id":"da5ab109-c803-430a-a902-7d29a860cfa8","title":"Progress Course","description":"Manual progress flow","teacherId":"5292d3b5-2bd4-462c-9c7c-48a8d5f28cf1","createdAt":"2026-04-28T20:00:00Z","tags":["progress"]}],"page":{"limit":20,"returned":1,"nextCursor":null},"links":{"self":"/courses/me"}}
```
- Errors:
  - `401` unauthenticated
  - `403` student
- Verified statuses: `200, 401, 403`

## `PATCH /courses/{courseId}`
- Auth: required
- Roles: `TEACHER`, `ADMIN`
- Path params:
  - `courseId` UUID required
- Request body: same as `POST /courses`
- Notes: submitted `tags` replace the full course tag set.
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

## `GET /courses/{courseId}/progress/me`
- Auth: required
- Roles: any authenticated user
- Path params:
  - `courseId` UUID required
- Behavior: returns the current authenticated user's progress in the course.
- Progress model: lecture-based. `progressPercent = completedLectures / totalLectures * 100`.
- Success `200`:
```json
{"courseId":"9b5657d9-c323-4a65-b433-215c06e25929","userId":"0c4afb8e-4263-4602-b645-5f1f0fa9cbc2","totalLectures":2,"completedLectures":1,"progressPercent":50.0,"completedLectureIds":["2c1743a0-66a8-4d6d-b0dc-cd3fefc430b4"],"lastCompletedAt":"2026-04-28T20:10:00Z"}
```
- Errors:
  - `401` unauthenticated
  - `400` student is not enrolled in the course
  - `404` unknown course
- Verified statuses: `200, 400, 401, 404`

## `GET /courses/{courseId}/users/{userId}/progress`
- Auth: required
- Roles: `ADMIN`, course owner `TEACHER`, or the user who owns `{userId}`
- Path params:
  - `courseId` UUID required
  - `userId` UUID required
- Behavior: returns progress for a specific user in a specific course.
- Success `200`: same response shape as `GET /courses/{courseId}/progress/me`.
- Errors:
  - `401` unauthenticated
  - `403` unrelated teacher/user
  - `400` target student is not enrolled in the course
  - `404` unknown course or user
- Verified statuses: `200, 400, 401, 403, 404`

## `PUT /courses/{courseId}/users/{userId}/progress/lectures/{lectureId}`
- Auth: required
- Roles: `ADMIN`, course owner `TEACHER`, or the user who owns `{userId}`
- Path params:
  - `courseId` UUID required
  - `userId` UUID required
  - `lectureId` UUID required
- Request body:
  - `completed` boolean required
- Example request:
```json
{"completed":true}
```
- Behavior:
  - `completed:true` creates the completion row if it does not already exist.
  - `completed:false` removes the completion row if present.
  - Response returns recalculated course progress.
- Success `200`:
```json
{"courseId":"9b5657d9-c323-4a65-b433-215c06e25929","userId":"0c4afb8e-4263-4602-b645-5f1f0fa9cbc2","totalLectures":2,"completedLectures":1,"progressPercent":50.0,"completedLectureIds":["2c1743a0-66a8-4d6d-b0dc-cd3fefc430b4"],"lastCompletedAt":"2026-04-28T20:10:00Z"}
```
- Errors:
  - `401` unauthenticated
  - `403` unrelated teacher/user
  - `400` target student is not enrolled in the course, or lecture does not belong to the course
  - `404` unknown course, user, or lecture
  - `422` missing `completed`
- Verified statuses: `200, 400, 401, 403, 404, 422`

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
- Validation error `422`: `questionCount` below 3 or above 20.
- Error `400`:
```json
{"type":"https://learning-platform/errors/bad-request","title":"Bad Request","status":400,"detail":"Use lessonId or lectureId, not both","instance":"/assessments/generate","correlationId":"9b3e4582-898d-4759-8aeb-c300deedad2e","timestamp":"2026-04-20T18:56:21.738855163Z"}
```
- Verified statuses: `200, 400, 422`

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
Sources:
- `/tmp/manual_api_results_manual1777311128.tsv`
- `/tmp/manual_api_bodies_manual1777311128`
- `/tmp/manual_api_admin_enrollment_cleanup_manual1777311128.json`
- `/tmp/self_profile_self1777406396`
- `/tmp/progress_flow_progress1777406834`
- `/tmp/courses_me_mine1777407095`
- `/tmp/full_api_flow_full1777407269.tsv`
- `/tmp/full_api_flow_full1777407269`

- Every business endpoint in scope was invoked at least once.
- Role-based positive and negative cases were executed for protected endpoints.
- `GET /users/{userId}` was verified for admin-or-self profile access.
- `/token` verified with successful and invalid-credential flows.
- Course tag normalization, replacement, and OR-style `tag` filtering were manually verified.
- `GET /courses/me` returns authored courses using the local user resolved from JWT `sub`; clients should use it for teacher dashboards instead of filtering by JWT identifiers.
- Course progress is tracked through `lecture_progress` and computed from completed lectures per course.
- Admin management behavior was manually verified for user directory/details, course create/patch/delete, lesson/lecture creation, assessment creation, submission grading, and enrollment create/delete.
- The only failed intermediate matrix line was an intentionally invalid `POST /assessments/generate` request with `questionCount=2`; corrected request with `questionCount=3` passed with `200`.
