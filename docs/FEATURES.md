# Skultem API Feature Notes

## Production Setup

- First-deploy bootstrap (infra, secrets, first school, first `SYSTEM_ADMIN`):
  `docs/PRODUCTION_BOOTSTRAP.md`

## Library Attendance

- Requirements, decisions and how it was built: `docs/LIBRARY_ATTENDANCE.md`
- Base path `/api/v1/library/attendance` (Library module). Samples: `docs/library_attendance.http`
  - Visits: `POST /visits/check-in`, `POST /visits/{id}/check-out`, `POST /visits/{id}/resolve`, `GET /visits`
  - Class sessions: `GET /sessions?from&to`, `POST /sessions/open`, `PUT /sessions/{id}/marks`,
    `POST /sessions/{id}/roster`, `POST /sessions/{id}/finalize`, `POST /sessions/{id}/cancel`,
    `POST /sessions/cancel`, `POST /sessions/{id}/records/{recordId}/correct`
  - Reports: `GET /overview`, `GET /totals`, `GET /students/{id}/history`, `GET /export/*`

## Class And Stream Subject Selection

- `GET /api/v1/class/subject/{classId}`
  - Returns subject selection payload:
    - `core`: mandatory subjects
    - `options`: grouped optional subjects (`groupId`, `name`, `select`, `list`)
- For `SSS` classes, pass stream id:
  - `GET /api/v1/class/subject/{classId}?streamId={streamId}`
  - `streamId` is required for `SSS`.

## Subject Assignment

- Assign subjects to class:
  - `POST /api/v1/assignment/class/{classId}`
- Assign subjects to stream:
  - `POST /api/v1/assignment/stream/{streamId}`
- Request body shape:
  - `assignments[].subjectId`
  - `assignments[].mandatory`
  - `assignments[].groupId` (for optional grouped subjects)

## Student Creation With Option Selections

- `POST /api/v1/student`
- Request body:
  - `givenNames`
  - `familyName`
  - `academicNumber`
  - `gender`
  - `dateOfBirth`
  - `classSessionId`
  - `selectedOptionIds` (optional list of selected optional subjects)
- Behavior:
  - Core subjects are always enrolled.
  - Optional selections are validated against configured groups.
  - Fees and ledger entries are created from applicable fee structures.

## Existing HTTP Samples

- Class endpoints: `docs/class.http`
- Stream endpoints: `docs/stream.http`
- Subject assignment: `docs/subject_assignment.http`
- Student endpoints: `docs/student.http`

## Seed Test Data (Up To Student Enrollment)

- Run:
  - `bash scripts/seed_enroll_student.sh`
- Defaults:
  - `BASE_URL=http://localhost:8080/api/v1`
  - `USER_ID=USR-2026-0001`
  - `SCHOOL_ID=SCL-2026-0001`
- What it seeds:
  - academic year (and activates it)
  - term
  - section
  - subjects
  - assessment template + assignments
  - class linked to assessment template
  - subject group + class subject assignments
  - student creation with optional subject selection
