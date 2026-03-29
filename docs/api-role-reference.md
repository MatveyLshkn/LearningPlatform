 # API Role Cases (Detailed)

This file provides concrete request/response examples for every endpoint and role:
- `ADMIN`
- `TEACHER`
- `USER` (`STUDENT`)

Base URL:
```json
{
  "baseUrl": "http://localhost:8080"
}
```

Tokens (examples):
```json
{
  "adminToken": "eyJ...admin",
  "teacherToken": "eyJ...teacher",
  "userToken": "eyJ...user"
}
```

Entity IDs used below:
```json
{
  "teacherOwnCourseId": "3d1e1c08-9434-4fd7-8f07-4729c8f39572",
  "otherTeacherCourseId": "0bd11089-765b-46c3-8c79-33fb1dd36700",
  "lessonId": "7922e34a-e7e6-4e87-a39a-b54a7be8457b",
  "assessmentId": "451293a8-9ce0-4a0f-8dce-97e00188585e",
  "submissionId": "94e241cb-48e1-4096-96ae-164fe90148d4",
  "userOwnEnrollmentId": "7a3d820e-aba2-4ab9-a4ff-a6e1d0ad9ba6",
  "otherUserEnrollmentId": "4cba837b-5348-4c65-89e5-ccf744db60f0",
  "targetUserId": "4bc0b1f1-5e4c-46a2-90c1-fda982a7600e"
}
```

Common forbidden error:
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access is denied",
  "instance": "/endpoint",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

Common unauthorized error:
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401
}
```

## `POST /token`
Request:
```json
{
  "username": "admin",
  "password": "admin"
}
```

`ADMIN` success (`200`):
```json
{
  "accessToken": "eyJ...admin",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "refreshToken": "eyJ...refresh",
  "refreshExpiresIn": 1800,
  "scope": "openid profile email"
}
```

`TEACHER` success (`200`):
```json
{
  "accessToken": "eyJ...teacher",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "refreshToken": "eyJ...refresh",
  "refreshExpiresIn": 1800,
  "scope": "openid profile email"
}
```

`USER` success (`200`):
```json
{
  "accessToken": "eyJ...user",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "refreshToken": "eyJ...refresh",
  "refreshExpiresIn": 1800,
  "scope": "openid profile email"
}
```

Invalid credentials (`400`):
```json
{
  "type": "https://learning-platform/errors/bad-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid username or password",
  "instance": "/token",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

## `POST /user-registrations`
Request:
```json
{
  "username": "teacherNew",
  "email": "teacherNew@example.com",
  "password": "Password123!",
  "role": "TEACHER"
}
```

Any role / anonymous success (`201`):
```json
{
  "userId": "8bdd1c62-618f-474b-b88c-722ea887bbcc",
  "keycloakSub": "848eafaa-d7ce-47be-9d78-2999e06e1142",
  "username": "teacherNew",
  "email": "teacherNew@example.com",
  "role": "TEACHER"
}
```

Trying to self-assign admin (`400`):
```json
{
  "type": "https://learning-platform/errors/bad-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "ADMIN cannot be self-assigned",
  "instance": "/user-registrations",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

## `GET /users/{userId}`
### ADMIN (`200`)
```json
{
  "id": "4bc0b1f1-5e4c-46a2-90c1-fda982a7600e",
  "username": "studentA",
  "email": "studentA@example.com",
  "role": "STUDENT",
  "createdAt": "2026-03-25T18:00:00Z"
}
```

### TEACHER (`200`)
```json
{
  "id": "4bc0b1f1-5e4c-46a2-90c1-fda982a7600e",
  "username": "studentA",
  "email": "studentA@example.com",
  "role": "STUDENT",
  "createdAt": "2026-03-25T18:00:00Z"
}
```

### USER (`403`)
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access is denied",
  "instance": "/users/4bc0b1f1-5e4c-46a2-90c1-fda982a7600e",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

## `POST /courses`
Request:
```json
{
  "title": "Java Architecture",
  "description": "Advanced backend course"
}
```

### ADMIN (`201`)
```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "title": "Java Architecture",
  "description": "Advanced backend course",
  "teacherId": "2b2261fb-c337-46a5-94f1-af1f32a8e111",
  "createdAt": "2026-03-25T18:00:00Z"
}
```

### TEACHER (`201`)
```json
{
  "id": "22222222-2222-2222-2222-222222222222",
  "title": "Java Architecture",
  "description": "Advanced backend course",
  "teacherId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "createdAt": "2026-03-25T18:00:00Z"
}
```

### USER (`403`)
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access is denied",
  "instance": "/courses",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

## `GET /courses`
### ADMIN / TEACHER / USER (`200`)
```json
{
  "items": [
    {
      "id": "3d1e1c08-9434-4fd7-8f07-4729c8f39572",
      "title": "Backend Architecture",
      "description": "Core Java course",
      "teacherId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "createdAt": "2026-03-25T18:00:00Z"
    }
  ],
  "page": {
    "limit": 20,
    "size": 1,
    "cursor": null
  },
  "links": {
    "self": "/courses"
  }
}
```

## `PATCH /courses/{courseId}`
Request:
```json
{
  "title": "Backend Architecture Updated",
  "description": "Updated"
}
```

### ADMIN (`200`)
```json
{
  "id": "3d1e1c08-9434-4fd7-8f07-4729c8f39572",
  "title": "Backend Architecture Updated",
  "description": "Updated",
  "teacherId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "createdAt": "2026-03-25T18:00:00Z"
}
```

### TEACHER owner (`200`)
```json
{
  "id": "3d1e1c08-9434-4fd7-8f07-4729c8f39572",
  "title": "Backend Architecture Updated",
  "description": "Updated",
  "teacherId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "createdAt": "2026-03-25T18:00:00Z"
}
```

### TEACHER non-owner (`403`)
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "You can update only your own course",
  "instance": "/courses/3d1e1c08-9434-4fd7-8f07-4729c8f39572",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

### USER (`403`)
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access is denied",
  "instance": "/courses/3d1e1c08-9434-4fd7-8f07-4729c8f39572",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

## `DELETE /courses/{courseId}`
### ADMIN (`204`)
```json
{}
```

### TEACHER owner (`204`)
```json
{}
```

### TEACHER non-owner (`403`)
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "You can delete only your own course",
  "instance": "/courses/0bd11089-765b-46c3-8c79-33fb1dd36700",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

### USER (`403`)
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access is denied",
  "instance": "/courses/0bd11089-765b-46c3-8c79-33fb1dd36700",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

## `POST /lessons`
Request:
```json
{
  "courseId": "3d1e1c08-9434-4fd7-8f07-4729c8f39572",
  "title": "Lesson One",
  "content": "Lesson content"
}
```

`ADMIN` (`201`), `TEACHER owner` (`201`) response:
```json
{
  "id": "7922e34a-e7e6-4e87-a39a-b54a7be8457b",
  "courseId": "3d1e1c08-9434-4fd7-8f07-4729c8f39572",
  "title": "Lesson One",
  "content": "Lesson content"
}
```

`TEACHER non-owner` (`403`):
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Only course teacher or admin can create lessons",
  "instance": "/lessons",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

`USER` (`403`):
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access is denied",
  "instance": "/lessons",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

## `GET /courses/{courseId}/lessons`
`ADMIN` / `TEACHER` / `USER` (`200`):
```json
[
  {
    "id": "7922e34a-e7e6-4e87-a39a-b54a7be8457b",
    "courseId": "3d1e1c08-9434-4fd7-8f07-4729c8f39572",
    "title": "Lesson One",
    "content": "Lesson content"
  }
]
```

## `POST /lectures`
Request:
```json
{
  "lessonId": "7922e34a-e7e6-4e87-a39a-b54a7be8457b",
  "title": "Lecture One",
  "videoUrl": "https://video.example.com/1",
  "content": "Lecture content"
}
```

`ADMIN` (`201`), `TEACHER owner` (`201`) response:
```json
{
  "id": "33333333-3333-3333-3333-333333333333",
  "lessonId": "7922e34a-e7e6-4e87-a39a-b54a7be8457b",
  "title": "Lecture One",
  "videoUrl": "https://video.example.com/1",
  "content": "Lecture content"
}
```

`TEACHER non-owner` (`403`) and `USER` (`403`) follow same forbidden format.

## `GET /lessons/{lessonId}/lectures`
`ADMIN` / `TEACHER` / `USER` (`200`):
```json
[
  {
    "id": "33333333-3333-3333-3333-333333333333",
    "lessonId": "7922e34a-e7e6-4e87-a39a-b54a7be8457b",
    "title": "Lecture One",
    "videoUrl": "https://video.example.com/1",
    "content": "Lecture content"
  }
]
```

## `POST /assessments`
Request:
```json
{
  "courseId": "3d1e1c08-9434-4fd7-8f07-4729c8f39572"
}
```

`ADMIN` (`201`), `TEACHER owner` (`201`) response:
```json
{
  "id": "451293a8-9ce0-4a0f-8dce-97e00188585e",
  "courseId": "3d1e1c08-9434-4fd7-8f07-4729c8f39572",
  "createdBy": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "createdAt": "2026-03-25T18:00:00Z"
}
```

`TEACHER non-owner` (`403`) detail:
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Only course teacher or admin can create assessments",
  "instance": "/assessments",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

`USER` (`403`) uses standard forbidden body.

## `GET /courses/{courseId}/assessments`
`ADMIN` / `TEACHER` / `USER` (`200`):
```json
[
  {
    "id": "451293a8-9ce0-4a0f-8dce-97e00188585e",
    "courseId": "3d1e1c08-9434-4fd7-8f07-4729c8f39572",
    "createdBy": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "createdAt": "2026-03-25T18:00:00Z"
  }
]
```

## `POST /enrollments`
Request:
```json
{
  "courseId": "3d1e1c08-9434-4fd7-8f07-4729c8f39572"
}
```

`ADMIN` (`201`) response:
```json
{
  "id": "44444444-4444-4444-4444-444444444444",
  "userId": "2b2261fb-c337-46a5-94f1-af1f32a8e111",
  "courseId": "3d1e1c08-9434-4fd7-8f07-4729c8f39572",
  "enrolledAt": "2026-03-25T18:00:00Z"
}
```

`TEACHER` (`403`) detail:
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Teachers cannot enroll as students",
  "instance": "/enrollments",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

`USER` first time (`201`) response:
```json
{
  "id": "7a3d820e-aba2-4ab9-a4ff-a6e1d0ad9ba6",
  "userId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "courseId": "3d1e1c08-9434-4fd7-8f07-4729c8f39572",
  "enrolledAt": "2026-03-25T18:00:00Z"
}
```

`USER` duplicate (`409`):
```json
{
  "type": "https://learning-platform/errors/conflict",
  "title": "Conflict",
  "status": 409,
  "detail": "User already enrolled in this course",
  "instance": "/enrollments",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

## `GET /enrollments`
`ADMIN` / `TEACHER` / `USER` (`200`, own enrollments only):
```json
[
  {
    "id": "7a3d820e-aba2-4ab9-a4ff-a6e1d0ad9ba6",
    "userId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
    "courseId": "3d1e1c08-9434-4fd7-8f07-4729c8f39572",
    "enrolledAt": "2026-03-25T18:00:00Z"
  }
]
```

## `DELETE /enrollments/{enrollmentId}`
`ADMIN` any enrollment (`204`):
```json
{}
```

`USER` own enrollment (`204`):
```json
{}
```

`USER` other user enrollment (`403`):
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "You can leave only your own enrollment",
  "instance": "/enrollments/4cba837b-5348-4c65-89e5-ccf744db60f0",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

`TEACHER` (`403`) uses standard forbidden body.

## `POST /submissions`
Request:
```json
{
  "assessmentId": "451293a8-9ce0-4a0f-8dce-97e00188585e",
  "answerText": "My answer"
}
```

`ADMIN` (`201`) and `USER` (`201`) response:
```json
{
  "id": "94e241cb-48e1-4096-96ae-164fe90148d4",
  "assessmentId": "451293a8-9ce0-4a0f-8dce-97e00188585e",
  "studentId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "answerText": "My answer",
  "score": null,
  "submittedAt": "2026-03-25T18:00:00Z",
  "gradedAt": null
}
```

`TEACHER` (`403`) detail:
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Teachers cannot submit student answers",
  "instance": "/submissions",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

## `PATCH /submissions/{submissionId}`
Request:
```json
{
  "score": 95
}
```

`ADMIN` (`200`) and `TEACHER owner` (`200`) response:
```json
{
  "id": "94e241cb-48e1-4096-96ae-164fe90148d4",
  "assessmentId": "451293a8-9ce0-4a0f-8dce-97e00188585e",
  "studentId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
  "answerText": "My answer",
  "score": 95,
  "submittedAt": "2026-03-25T18:00:00Z",
  "gradedAt": "2026-03-25T18:30:00Z"
}
```

`TEACHER non-owner` (`403`) detail:
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Only course teacher or admin can grade submissions",
  "instance": "/submissions/94e241cb-48e1-4096-96ae-164fe90148d4",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

`USER` (`403`) uses standard forbidden body.

## `GET /submissions/me`
`ADMIN` / `TEACHER` / `USER` (`200`, own submissions only):
```json
[
  {
    "id": "94e241cb-48e1-4096-96ae-164fe90148d4",
    "assessmentId": "451293a8-9ce0-4a0f-8dce-97e00188585e",
    "studentId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
    "answerText": "My answer",
    "score": 95,
    "submittedAt": "2026-03-25T18:00:00Z",
    "gradedAt": "2026-03-25T18:30:00Z"
  }
]
```

## `GET /submissions/assessment/{assessmentId}`
`ADMIN` / `TEACHER` / `USER` (`200`):
```json
[
  {
    "id": "94e241cb-48e1-4096-96ae-164fe90148d4",
    "assessmentId": "451293a8-9ce0-4a0f-8dce-97e00188585e",
    "studentId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
    "answerText": "My answer",
    "score": 95,
    "submittedAt": "2026-03-25T18:00:00Z",
    "gradedAt": "2026-03-25T18:30:00Z"
  }
]
```

## `GET /platform-statistics`
`ADMIN` (`200`):
```json
{
  "usersCount": 15,
  "coursesCount": 5,
  "enrollmentsCount": 12,
  "averageSubmissionScore": 87.5
}
```

`TEACHER` (`403`):
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access is denied",
  "instance": "/platform-statistics",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```

`USER` (`403`):
```json
{
  "type": "https://learning-platform/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access is denied",
  "instance": "/platform-statistics",
  "correlationId": "uuid",
  "timestamp": "2026-03-25T21:15:03.871165+03:00"
}
```
