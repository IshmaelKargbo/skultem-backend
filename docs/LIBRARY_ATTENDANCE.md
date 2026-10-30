# Library Attendance — Business Requirements

Status: **Built (backend and web app), 2026-10-30.** All decisions in §11 are settled. What was
built and how it was checked: §14.
Last updated: 2026-10-30.

This document is the single source of requirements for Library Attendance. Any
API notes added later should go into `FEATURES.md` and link back here, not
repeat these rules.

Nothing in this document has been built or tested yet. The "Existing system"
notes record what was found in the codebase on the date above.

---

## 1. Purpose and scope

Library Attendance lets a school record **who is in the library and when**. It covers
two separate workflows:

| Workflow | What it records | Time detail |
|---|---|---|
| **A. Independent library visits** | A student who comes to the library outside a scheduled class library period | Check-in and check-out timestamps per student |
| **B. Scheduled class library attendance** | Which students in a class attended a Library period that is already on the school's Skultem timetable | One status per student per session; no per-student times in the MVP |

Library Attendance **manages attendance only. It is not a library-management system.**

Skultem already has a separate, shipped library feature for book cataloguing and loans
(`LibraryController`, migrations `V109`–`V111`). Library Attendance does not add to it,
change it or depend on it. Book catalogue, borrowing, returns, fines and overdue handling
stay outside this specification (see §9).

### Core principles

1. The two workflows are kept apart. A record belongs to exactly one workflow and
   is counted only in that workflow.
2. Library Attendance never writes to ordinary school attendance (the `attendances` table and
   `MarkClassSessionAttendanceUseCase`), and it never sends parent notifications.
3. The system never invents data. It does not create automatic absences or check-out times, and it
   does not close records without someone resolving them.
4. History is preserved. Corrections and resolutions add to the record and never overwrite the
   original.
5. Every operation is limited to the caller's school.

---

## 2. Existing system — verified dependencies

These facts were checked in the backend source on 2026-10-29. They limit the design
choices below.

### 2.1 Tenancy

- Every school-owned table has a `school_id`. Controllers take the school from the
  authenticated principal (`@AuthenticationPrincipal(expression = "activeSchoolId")`), not from
  the request body. Repositories look records up by `…AndSchoolId`.
- Section-limited admins are further restricted by `SectionScopeGuard` / `@SectionScoped`
  (`@sectionScope.classSession(#school, #id)`).
- Modules are switched on per school with `@RequiresModule(FeatureModule.X)`.
  `FeatureModule.LIBRARY` already exists and gates `LibraryController`.

### 2.2 Roles and permissions

- Permissions are **role-based**. `Role` enum: `OWNER, SYSTEM_ADMIN, PROPRIETOR, PRINCIPAL,
  SUPER_ADMIN, ADMIN, ACCOUNTANT, LIBRARIAN, TEACHER, PARENT`. Gates are written as
  `@PreAuthorize("@permissionService.hasAnySchoolRole(#school, …)")`, with record-level checks
  added where needed (e.g. `canAccessClassSessionAsTeacher`).
- There are **no fine-grained per-capability school permissions**. Fine-grained platform
  permissions exist only for platform/system staff (`hasPlatformPermission`).
- `SUPER_ADMIN` automatically passes any gate that names `ADMIN, OWNER, PROPRIETOR, PRINCIPAL,
  ACCOUNTANT` or `LIBRARIAN` (`PermissionService.SUPER_ADMIN_COVERS`).
- `LIBRARIAN` was added in `V111` and today only uses the library portal.
- ⚠️ The existing library `VIEW` gate **includes `TEACHER`**. Library Attendance must **not**
  reuse that gate (see §4).

### 2.3 Timetable

- `Timetable` (table `timetables`) is a **weekly recurring grid**. One entry is
  `Period × WorkingDay → TeacherSubject + Room (+ colour)`.
- `Period` belongs to a single `ClassSession` (class + section + stream + academic year), has
  a `type` of `PERIOD | BREAK | LUNCH`, a name, and start/end `LocalTime`.
- `WorkingDay` is a weekday (`MONDAY…SUNDAY`) with an on/off `state`, tied to a `Timing`
  template that is picked per school level.
- `TeacherSubject` links a `ClassSession`, a `Teacher` and a `Subject`. **Every timetable
  entry therefore needs a teacher assigned to the subject.**
- `Subject` has only `name`, `code` and `description`. **Nothing marks a subject as "Library".**
- One period/day slot can hold **several entries side by side** (optional subjects), as long as
  the subject differs (`CreateTimetableUseCase`).
- Timetable entries have **no calendar dates, no recurrence rules, no exceptions and no
  cancellations**. Entries are edited in place (`Timetable.update`) and
  **hard-deleted** (`DeleteTimetableEntryUseCase`). No history is kept.
- Calendar context that exists elsewhere: `Term` (start/end dates), `Holiday` (dated, PUBLIC or
  SCHOOL) and `CalendarEvent`.
- Time zone: all schools are treated as `Africa/Freetown` (`SchoolTimeZone.ZONE`, UTC+0).

### 2.4 Class roster

- A student's placement is an `Enrollment` (student, class, section, stream, academic year,
  `status: ACTIVE | PROMOTED | REPEATED | LEFT`).
- **There are no effective dates or placement history.** `ChangeEnrollmentClassUseCase` moves
  a student by overwriting the enrollment row, and it **deletes that student's ordinary
  attendance rows**. A student's exit is recorded as `exit_date` / `exit_reason` (`V63`).
- As a result, **Skultem cannot rebuild a class roster as it was on a past date.** That
  is why scheduled sessions need a roster snapshot (§6.4).

### 2.5 Audit and activity

- `@AuditLogAnnotation(action = "…")` on a use case writes an `audit_logs` row through
  `AuditAspect`. The row holds the actor, school, IP and user agent, a status
  (`SUCCESS | FAILURE | REJECTED`), and a free-text `details` value (method args and result,
  truncated to 2000 characters).
- `audit_logs` is **append-only**, enforced by a DB trigger (`V99`).
- `LogActivityUseCase` writes a human-readable activity feed entry. `ActivityType.LIBRARY` exists.
- The audit log does **not** store structured before/after values. Where these requirements need
  original-vs-corrected values, they must be kept on Library Attendance's own records
  (§7), with the audit log as the tamper-evident trail of who acted and when.

### 2.6 Other existing patterns to reuse

- `@Idempotent(operation = "…")` gives safe retries for create-style requests.
- Exports: `ReportExportService` produces **CSV and PDF**.
- Student search: `GET /api/v1/student?search=…` already allows `LIBRARIAN`.
- Ordinary attendance has a `late` flag. Library Attendance does not, unless decided otherwise (§11).

---

## 3. Users

| User | Role(s) in Skultem |
|---|---|
| Librarian | `LIBRARIAN` |
| Principal / authorized administrator ("principal-level") | `PRINCIPAL`, `OWNER`, `PROPRIETOR`, `SUPER_ADMIN` (D-1). Plain `ADMIN` can view only |
| Teacher | `TEACHER`. No Library Attendance access by default |
| Parent | `PARENT`. No access in the MVP |

No new identity system and no new role hierarchy are introduced.

---

## 4. Permissions

Capabilities map onto the existing `hasAnySchoolRole` gates. Every operation also requires
`FeatureModule.LIBRARY` (D-2), applies the school from the principal,
and applies `@sectionScope` for class-session-based operations.

| Capability | LIBRARIAN | PRINCIPAL | OWNER / PROPRIETOR / SUPER_ADMIN | ADMIN | TEACHER |
|---|---|---|---|---|---|
| Search/select students | ✅ | ✅ | ✅ | ✅ | ❌ |
| Check in (single/bulk), check out | ✅ | ❌ | `SUPER_ADMIN` only (P-5) | ❌ | ❌ |
| View active visits, history, unresolved visits | ✅ | ✅ | ✅ | ✅ | ❌ |
| Resolve a forgotten check-out | ✅ | ✅ | ✅ | ❌ | ❌ |
| View scheduled library sessions | ✅ | ✅ | ✅ | ✅ | ❌ |
| Open session, mark attendance, adjust roster, finalize | ✅ | ❌ | `SUPER_ADMIN` only (P-5) | ❌ | ❌ |
| Cancel a session | ✅ | ✅ | ✅ | ❌ | ❌ |
| Correct finalized attendance | ❌ | ✅ | ✅ | ❌ | ❌ |
| Manage timetable (incl. Library periods) | ❌ | ✅ (existing gate) | ✅ (existing gate) | ✅ (existing gate) | ❌ |
| View reports | ✅ | ✅ | ✅ | ✅ | ❌ |
| Export reports (CSV, PDF) | ✅ | ✅ | ✅ | ❌ | ❌ |

Rules:

- **P-1** Library Attendance uses its own gate constants. It must not reuse the existing
  library `VIEW` gate, which includes `TEACHER`.
- **P-2** A teacher gets no Library Attendance access unless one of their roles is
  explicitly added to a gate by a decision recorded here.
- **P-3** A librarian cannot correct finalized attendance.
- **P-4** Every read and write is limited to the caller's active school. A request for an
  ID from another school behaves as "not found". It never returns a forbidden error that would
  confirm the record exists.
- **P-5** `SUPER_ADMIN` gets librarian capabilities automatically through
  `SUPER_ADMIN_COVERS`. This is existing behaviour and is accepted (D-1).
- **P-6** Recording (check-in/out, marking, roster adjustment, finalizing) is librarian work.
  `PRINCIPAL`, `OWNER` and `PROPRIETOR` supervise: they view, resolve, cancel and correct,
  but they do not record (D-1).
- **P-7** Librarians are not limited to a management section in the MVP (D-19). Library
  Attendance is not section-scoped, so a section-limited admin can't open it at all. That
  matches the rest of the Library module today.
- **P-8** Library periods are added and removed on the timetable, so the existing timetable
  permissions apply: `ADMIN`, `PRINCIPAL`, `SUPER_ADMIN`, `OWNER` and `PROPRIETOR`. The
  librarian doesn't edit the timetable; they see and start the sessions it produces.

---

## 5. Workflow A — Independent library visits

### 5.1 Functional requirements

| ID | Requirement |
|---|---|
| A-1 | A librarian can search for a student by name or academic number and select them manually. |
| A-2 | A librarian can select several students and check them in with one action. |
| A-3 | Each check-in records the student, the school, the check-in timestamp (server time) and the user who checked them in. |
| A-4 | A librarian can see all currently active (open) visits for the school. |
| A-5 | A librarian can check out a student with an active visit. This records the check-out timestamp and the user who checked them out. |
| A-6 | A librarian can view past visits, filtered by date range, student and class. |
| A-7 | Visits still open after the end of the school day (§5.3) appear in a separate **unresolved visits** list. |
| A-8 | A librarian or principal-level user can resolve an unresolved visit. Resolving keeps the original visit unchanged and adds a resolution (§5.3). |

### 5.2 Business rules

- **AR-1 One active visit.** A student can have at most one active independent visit at a
  school at a time. The database layer must enforce this, not just a check made
  before saving, so two requests at the same moment cannot both succeed.
- **AR-2 Repeat visits.** Once a visit is closed (checked out or resolved), the student can be
  checked in again.
- **AR-3 Duration.** Duration = check-out timestamp − check-in timestamp. It is only calculated
  for visits closed by a real check-out. Visits closed by a resolution have no check-out time
  and no duration, and they are left out of duration totals.
- **AR-4 No silent closing.** The system never closes open visits automatically and never
  fills in check-out times. Open visits stay open and visible until someone resolves them.
- **AR-5 Bulk check-in is all-or-nothing.** A bulk check-in either saves every selected student
  or saves none. If any selected student cannot be checked in (already active, not enrolled,
  belongs to another school), the whole request is refused and the response lists each
  student and the reason. The screen then offers **"Check in eligible students only"** as a
  separate action the librarian must confirm (D-5). That second request is also all-or-nothing
  for the students it covers.
- **AR-6 Retries.** Repeating a check-in or check-out request (e.g. after a network
  timeout) must not create a second record. Use the existing `@Idempotent` mechanism.
- **AR-7 Eligibility (D-12).** Only students with an `ACTIVE`
  enrollment in the school's active academic year can be checked in. Withdrawn/expelled
  students (enrollment `LEFT`) cannot.
- **AR-8 Separation from scheduled sessions.** Independent visits are for time outside a
  student's scheduled library period. If a student's class has an *In progress* scheduled
  library session at check-in time, the librarian sees a warning but can still check the
  student in (D-11). An independent visit is never counted as scheduled-session
  attendance, and the reverse never happens either.
- **AR-9 Students only.** Visits by teachers or other staff are not recorded in the MVP.
- **AR-10 Snapshot of names.** Following the existing library-loan pattern (`V109`), the
  student's name and class at check-in are copied onto the visit. History therefore still reads
  correctly after a class change or exit.

### 5.3 Forgotten check-outs

- An **unresolved visit** is an open visit whose school day has ended (D-4). The school day ends
  at the `endTime` of the `Timing` template for the student's class level on the check-in date
  (the same template the timetable uses). Any open visit from an earlier date is always
  unresolved. A student checked in after that time (an after-school visit), or at a school with
  no timing set up, has until midnight.
- A librarian or a principal-level user can resolve it (D-3). A resolution records the
  resolving user, the resolution timestamp and a **required** reason. It closes the visit as
  "check-out not recorded". No check-out time is entered or estimated.
- The original check-in, and the fact that no check-out was recorded, are never overwritten.
- Each resolution writes an audit entry (`@AuditLogAnnotation`) and an activity entry
  (`ActivityType.LIBRARY`).
- Because of AR-1, an unresolved visit blocks a new check-in for that student until it is
  resolved. The check-in screen must say this and point to the resolution action.

---

## 6. Workflow B — Scheduled class library attendance

### 6.1 Identifying library periods

- **B-1** A scheduled library session comes from a **Library period** on the school's timetable
  (D-7, revised 2026-10-30). On the timetable, the school clicks a class's period and chooses
  "Library period" instead of a lesson: for example Class 1 after lunch on Friday, or Class 2 on
  Tuesday. Each Library period then shows up every week in the librarian's Class Sessions list.
  A school with no Library periods has no class sessions, and the screen says so.
- **B-2** There is no separate library timetable. Library periods are created, moved and removed
  only through the existing timetable screen and its permissions (P-8).
- **B-3** Only `Period.type = PERIOD` entries can be Library periods. Breaks and lunch never can.
- **B-4** A Library period has **no subject and no teacher** (D-8, revised). The librarian runs
  it. The timetable shows it as "Library" with "Librarian" in place of a teacher.
- **B-5** A Library period takes the whole period for the class (D-9, revised). It can't sit
  beside subject options in the same slot, and a subject can't be added to a slot that holds one.
  The roster is therefore always the whole class session.

### 6.2 Session instances

A **session** is one occurrence of a Library timetable entry on a specific date.

- **B-6** A date has a session for an entry when all of these hold: the weekday matches the
  entry's `WorkingDay`, the working day is switched on, the date falls inside a term of the
  active academic year, and the date is not a `Holiday`. Sessions on holiday dates are **not
  shown at all** (D-10). They are neither Upcoming nor Cancelled and never appear in reports.
  If a holiday is added for a date with an already-opened session, that session is unchanged
  and the librarian can cancel it as normal.
- **B-7** When a session is opened, its details are saved on the session record: date,
  class session, subject, period name, start/end time, room and source timetable entry.
  Editing or deleting the timetable entry afterwards does not change existing sessions.
  The source entry can be hard-deleted, so the link to it must be optional and must not
  block deletion.
- **B-8** Sessions that have not been opened yet are worked out from the current timetable.
  Only opened sessions are saved.

### 6.3 Session lifecycle

| Status | Meaning | Stored? |
|---|---|---|
| **Upcoming** | Scheduled, not yet opened | Worked out (not stored) |
| **In progress** | Opened by a librarian; attendance can be edited | Stored |
| **Completed** | Finalized; attendance is locked | Stored |
| **Cancelled** | Will not or did not take place; no attendance counts | Stored |

Transitions:

- Upcoming → In progress (librarian opens it)
- Upcoming → Cancelled, In progress → Cancelled (librarian or principal-level user, D-6;
  reason required). Cancelling an Upcoming session saves it so the cancellation is kept.
- In progress → Completed (finalize; requires zero `UNMARKED`)
- Completed → (no status change; cannot be cancelled; corrections only, §7)
- Cancelled → anything: **not allowed** (D-6). A cancellation cannot be undone in the MVP.

Rules:

- **B-9 Cancelled sessions create no absences.** Marks entered before cancellation are kept for
  history but are left out of every attendance total.
- **B-10 No automatic absences.** A session whose period has ended without being finalized is
  shown as **incomplete**, a flag worked out from Upcoming/In progress plus "end time passed".
  Nothing is marked absent automatically, and the session is not finalized automatically.
- **B-11 Late entry (D-13).** A session can be opened, marked and finalized after its period
  has ended, up to the end date of the term it falls in. After the term ends, an unfinalized
  session can no longer be opened or changed by the librarian. It stays listed as incomplete
  and is left out of status totals (R-2).

### 6.4 Roster

- **B-12** When a session is opened, the roster is copied from the students with an `ACTIVE`
  enrollment in the session's class session (class + section + stream + academic year). Each roster row stores the student ID plus their name and class at that moment.
- **B-13** The roster snapshot is the session's permanent historical roster. Later class
  changes, withdrawals or promotions never add, remove or change its rows.
- **B-14** Library attendance rows are **not** deleted by `ChangeEnrollmentClassUseCase` (which
  deletes ordinary attendance). This must be kept that way when the module is built.
- **B-15** Sessions on past dates: Skultem has no placement history (§2.4), so a session
  opened after its date uses the roster *as of when it is opened*. The screen must
  say so.
- **B-15a Roster adjustments (D-15).** Until a session is finalized, the librarian can add a
  student (same school, `ACTIVE` enrollment) to the roster or remove one from it. A reason is required.
  Each adjustment is stored with the user, the timestamp, the reason and whether the student was
  added or removed. It also writes an audit entry. A removed student's row is kept as removed (not
  deleted) and is not counted. After finalization the roster cannot be changed, only
  corrected through §7.

### 6.5 Attendance statuses

| Status | Final? | Meaning |
|---|---|---|
| `UNMARKED` | No | Starting working value for every roster row |
| `PRESENT` | Yes | Attended |
| `ABSENT` | Yes | Did not attend |
| `EXCUSED` | Yes | Did not attend, with a valid excuse. A reason can be entered but is optional (D-16) |

- **B-16** The librarian marks each student manually. A "mark all remaining as Present"
  shortcut is allowed only as an explicit action the librarian takes. It is never a default.
- **B-17** Before finalizing, the screen lists every `UNMARKED` student, and the librarian must
  mark each one Present, Absent or Excused.
- **B-18** A session with any `UNMARKED` row cannot be finalized. This is enforced on the server,
  not just in the UI.
- **B-19** Finalizing records the finalizing user and timestamp, and it is all-or-nothing.
- **B-19a** Finalizing also checks out every student on the roster who still has an open
  independent visit checked in on the session's date, using the finalize time and user. Open
  visits from earlier days are left alone, to be resolved as forgotten check-outs (§5.3).
- **B-20** No per-student check-in or check-out times are recorded for scheduled sessions in
  the MVP.
- **B-21** "Late" is not a status in the MVP (D-17). A late student is marked `PRESENT`.
- **B-22** Scheduled library attendance never updates ordinary school attendance and never sends
  parent notifications.
- **B-23** If two users mark or finalize the same session at the same moment, finalization
  happens at most once, and a stale edit cannot overwrite a finalized session (optimistic or
  row-level locking).

---

## 7. Finalization and corrections

- **C-1** Once a session is Completed, its attendance rows can only be changed through the
  correction action.
- **C-2** Only `PRINCIPAL`, `OWNER`, `PROPRIETOR` and `SUPER_ADMIN` can correct finalized
  attendance (D-1). `LIBRARIAN` and plain `ADMIN` cannot.
- **C-3** Each correction is stored as its own record with: session, student, original
  status, corrected status, the user who corrected it, the timestamp and a **required** reason.
  The attendance row shows the current status. The full correction history stays available and is
  never edited or deleted.
- **C-4** Each correction also writes an append-only `audit_logs` entry through
  `@AuditLogAnnotation` (e.g. `LIBRARY_ATTENDANCE_CORRECTED`) and an activity entry.
- **C-5** A correction cannot set a status back to `UNMARKED`.
- **C-6** Reports use the current (corrected) status and can show that a row was corrected.

---

## 8. MVP scope

Included:

- Manual student search and selection
- Bulk independent-visit check-in
- Independent-visit check-out
- Active visits and visit history
- Unresolved visits list and authorized resolution
- Scheduled library sessions taken from the existing timetable
- Session-specific class roster (snapshot)
- Manual Present / Absent / Excused marking
- Review and finalization of scheduled attendance
- Controlled corrections of finalized attendance, with history and audit
- Basic daily, class and student reports (§10)
- Lists of incomplete sessions and unresolved visits
- Existing role gates, module gating, section scope and school isolation

## 9. Explicitly outside the MVP

Not included unless separately approved:

- Book cataloguing, borrowing, returns, fines and overdue-book management *(these already
  exist as a separate Library feature and are not part of this module)*
- QR codes, barcodes or student self-check-in
- Automatic parent notifications
- Automatic absence detection
- Any update to ordinary school attendance
- Per-student timed attendance for scheduled sessions
- Offline attendance and later synchronization
- Reading goals, challenges or recommendations
- Teacher/staff library visits
- A separate library timetable

---

## 10. Reports

| Report | Contents | View | Export |
|---|---|---|---|
| Daily independent visits | Visits for a chosen date: student, class, check-in, check-out, duration, state (open / closed / resolved) | Per §4 "View reports" | Per R-3 |
| Active and unresolved visits | Currently open visits; visits past the follow-up threshold | Per §4 | Per R-3 |
| Scheduled attendance by class and session | Per session: roster with final status, corrections flagged; sessions listed as Upcoming / In progress / incomplete / Completed / Cancelled | Per §4 | Per R-3 |
| Student library history | For one student: independent visits **and** scheduled-session records, shown in **separate sections** | Per §4 | Per R-3 |
| Status totals | Counts of Present / Absent / Excused by class, session and date range (Completed sessions only) | Per §4 | Per R-3 |

Report rules:

- **R-1** Independent visits and scheduled attendance are never added into one combined
  "library attendance" number.
- **R-2** Cancelled sessions and incomplete (not finalized) sessions are left out of status
  totals. Incomplete sessions are listed separately so someone can follow them up.
- **R-3** Every report can be exported as CSV or PDF through the existing
  `ReportExportService`, by the librarian and principal-level roles (D-14). Plain `ADMIN` can
  view reports on screen but not export them.
- **R-4** Records are kept indefinitely (D-18), the same as the rest of Skultem today. No purge
  job is part of the MVP. Any future retention policy must leave `audit_logs` alone, because it
  is append-only.

---

## 11. Decisions

### 11.1 Approved (2026-10-29)

| # | Decision | Outcome |
|---|---|---|
| D-1 | Who records; who corrects | Record: `LIBRARIAN` only (+ `SUPER_ADMIN` via the existing rule). Correct finalized attendance: `PRINCIPAL, OWNER, PROPRIETOR, SUPER_ADMIN`. Plain `ADMIN` views only. See §4, C-2 |
| D-3 | Who resolves forgotten check-outs | Librarian and principal-level roles; reason required; no check-out time entered. See §5.3 |
| D-4 | When a visit becomes unresolved | At the end of the school day (`Timing.endTime` for the student's level). See §5.3 |
| D-6 | Cancelling sessions | Librarian and principal-level roles; reason required; Upcoming or In progress only; no undo. See §6.3 |
| D-7 | Identifying Library periods | **Revised 2026-10-30:** the school marks a period as a "Library period" directly on the timetable. (Was: a per-school Library-subject setting, now removed.) See B-1 |
| D-8 | Teacher requirement on timetable entries | **Revised 2026-10-30:** a Library period has no subject or teacher. (Was: any teacher as a placeholder.) See B-4 |
| D-9 | Roster when Library shares a slot | **Revised 2026-10-30:** a Library period takes the whole slot, so the roster is always the whole class. See B-5 |
| D-15 | Adjusting an open session's roster | Allowed for the librarian until finalization; reason required; every change recorded. See B-15a |

### 11.2 Approved (2026-10-30)

| # | Decision | Outcome |
|---|---|---|
| D-2 | Module switch | Covered by the existing `FeatureModule.LIBRARY`. Its description should be updated to mention attendance when the module is built |
| D-5 | After a refused bulk check-in | Offer "Check in eligible students only" as a separate confirmed action. See AR-5 |
| D-10 | Holidays | Sessions on holiday dates are not shown. See B-6 |
| D-11 | Independent check-in during the class's library session | Warn, don't block. See AR-8 |
| D-12 | Eligibility | `ACTIVE` enrollment in the active academic year only. See AR-7 |
| D-13 | Late entry | Allowed until the end of the session's term. See B-11 |
| D-14 | Exports | Librarian and principal-level roles; CSV and PDF; plain `ADMIN` views only. See R-3 |
| D-16 | Reason for `EXCUSED` | Optional |
| D-17 | Late arrivals | No separate status; marked `PRESENT`. See B-21 |
| D-18 | Retention | Kept indefinitely; no purge in the MVP. See R-4 |
| D-19 | Section-limited librarians | Not in the MVP. Librarians work across the whole school, and `@sectionScope` keeps applying to section-limited admins as it does today |

---

## 12. Implementation-readiness checklist

Blocking for technical design (approved 2026-10-29):

- [x] D-7 Library periods marked on the timetable
- [x] D-8 Teacher requirement for Library periods
- [x] D-1 Recording and correction roles
- [x] D-3 / D-4 Forgotten check-out policy
- [x] D-6 Cancellation rules
- [x] D-9 Whole-class roster (Library period takes the whole slot)
- [x] D-15 Roster adjustment rule

Required before release:

- [x] D-2, D-5, D-10 – D-14, D-16 – D-19 decided (2026-10-30)
- [x] Built: the single-active-visit rule enforced at the data layer (AR-1), atomic bulk
      check-in (AR-5), idempotent check-in (AR-6), the session/roster snapshot (B-7, B-12),
      roster adjustment history (B-15a), locking on finalize (B-23), correction history (C-3),
      and the Library-subject setting (B-1)
- [x] `ChangeEnrollmentClassUseCase` doesn't touch library attendance (B-14). Permanent
      student deletion, which is for students entered by mistake, removes that student's
      library attendance rows along with everything else.
- [x] Activity entries use `ActivityType.LIBRARY`; audit actions are listed in §14
- [x] Acceptance criteria exercised (§13, §14)
- [ ] The web screens clicked through by a librarian and a principal before release

---

## 13. Acceptance criteria

Each criterion was exercised on 2026-10-30, by automated tests and by calling the running backend
against a test database (§14). The web screens haven't been clicked through yet.

| # | Criterion | Requirements |
|---|---|---|
| AC-1 | A librarian can find a student and record an independent visit. | A-1, A-3 |
| AC-2 | A librarian can check in several students in one operation. | A-2, AR-5 |
| AC-3 | A student cannot have two active independent visits at the same school, including under simultaneous requests. | AR-1, AR-6 |
| AC-4 | Check-in and check-out timestamps give accurate visit durations; resolved visits are not counted as measured durations. | A-3, A-5, AR-3 |
| AC-5 | Forgotten check-outs stay visible and can be resolved without erasing history; each resolution is audited. | A-7, A-8, AR-4, §5.3 |
| AC-6 | Library periods on the timetable produce the expected sessions on the expected dates. | B-1, B-3, B-6 |
| AC-7 | An opened session shows the whole class as its roster, frozen as a snapshot. Roster adjustments before finalization need a reason and are recorded; none are possible after. | B-5, B-12, B-13, B-15, B-15a |
| AC-8 | Rows start as `UNMARKED`; only `PRESENT`, `ABSENT`, `EXCUSED` are final; `UNMARKED` is never a final outcome. | §6.5, C-5 |
| AC-9 | A session with any `UNMARKED` student cannot be finalized (server-enforced). | B-17, B-18 |
| AC-10 | Cancelled sessions add nothing to absence or status totals. | B-9, R-2 |
| AC-11 | A session whose period has ended unfinalized is flagged incomplete and creates no absences. | B-10 |
| AC-12 | Only authorized roles can correct finalized attendance; each correction stores original status, new status, actor, time and reason, and writes an audit entry. | C-1 – C-4 |
| AC-13 | Editing/deleting timetable entries or moving/removing students does not change past sessions, rosters or statuses. | B-7, B-13, B-14 |
| AC-14 | No operation reads or changes another school's students, visits, sessions or reports. | P-4 |
| AC-15 | Independent visits and scheduled attendance are stored, counted and reported separately and never written to ordinary attendance. | §1 principles, AR-8, B-22, R-1 |
| AC-16 | Failed, retried or simultaneous operations leave no duplicate or partly saved records (bulk check-in, finalize, corrections). | AR-1, AR-5, AR-6, B-19, B-23 |
| AC-17 | A `TEACHER` with no other role cannot reach any Library Attendance operation. | P-1, P-2 |

---

## 14. Implementation

### Where it lives

| Layer | Files |
|---|---|
| Schema | `V113__library_attendance.sql`: `library_visits`, `library_sessions`, `library_session_records`, `library_roster_changes`, `library_attendance_corrections`. `V114__timetable_library_periods.sql`: `timetables.kind` (`SUBJECT` / `LIBRARY`), teacher-subject optional for Library periods, one Library period per slot; drops V113's `library_settings.library_subject_id` |
| Domain | `LibraryVisit`, `LibrarySession`, `LibrarySessionRecord`, `LibraryRosterChange`, `LibraryAttendanceCorrection` (rules live here) |
| Use cases | `CheckInLibraryVisitorsUseCase`, `CheckOutLibraryVisitUseCase`, `ResolveLibraryVisitUseCase`, `OpenLibrarySessionUseCase`, `MarkLibrarySessionUseCase`, `AdjustLibraryRosterUseCase`, `FinalizeLibrarySessionUseCase`, `CancelLibrarySessionUseCase`, `CorrectLibraryAttendanceUseCase`; Library periods are saved by the existing `CreateTimetableUseCase` |
| Reads | `LibraryAttendanceService`; `LibrarySessionSchedule` (sessions from the timetable); `LibraryRosterResolver` (whole class) |
| API | `LibraryAttendanceController`, `/api/v1/library/attendance` (samples: `docs/library_attendance.http`) |
| Exports | `LibraryAttendanceExporter` (CSV / PDF through `ReportExportUtil`) |
| Web app | `/library/attendance` (visits), `/library/attendance/sessions` and `/sessions/[id]` (registers), `/library/attendance/reports`; Library periods are added in the timetable's add/edit dialog ("This period is: A lesson / Library period") |

### How the hard rules are enforced

- **One open visit (AR-1)**: a partial unique index, `uq_library_visits_one_open`. A check-in
  that loses a race gets "… is already checked in" and its whole request rolls back.
- **One session per period and date**: a unique index on (school, timetable entry, date).
  Opening an already-open session returns it.
- **Concurrent register changes (B-23)**: marking, roster changes, finalizing, cancelling and
  correcting all lock the session row first.
- **State**: database checks stop a resolved visit from carrying a check-out time, a cancelled
  session from lacking a reason, and a correction from recording `UNMARKED` or no change.
- **History**: a session keeps its own copy of class, period, time and room. Deleting the
  timetable entry only clears the link (`ON DELETE SET NULL`).

Audit actions: `LIBRARY_VISIT_CHECKED_IN`, `LIBRARY_VISIT_CHECKED_OUT`,
`LIBRARY_VISIT_RESOLVED`, `LIBRARY_SESSION_OPENED`, `LIBRARY_SESSION_MARKED`,
`LIBRARY_ROSTER_STUDENT_ADDED`, `LIBRARY_ROSTER_STUDENT_REMOVED`, `LIBRARY_SESSION_FINALIZED`,
`LIBRARY_SESSION_CANCELLED`, `LIBRARY_ATTENDANCE_CORRECTED`. Business refusals are logged as
`REJECTED`, not `FAILURE`.

### How it was checked (2026-10-30)

- 48 new unit tests: domain rules, check-in (all-or-nothing, eligible-only, race, warning),
  corrections, the session schedule, the roster rule and the timetable's Library-period rules. The full backend suite passes apart
  from 5 failures that existed before this work.
- The backend ran against a fresh test database. These were driven through the API:
  - Library periods added through the timetable API (Class 1 after lunch on Friday, Class 2 on
    Tuesday) and listed for the librarian every week; refused on a lunch break, beside a lesson,
    and a lesson refused beside a Library period; the timetable grid shows "Library" with no
    teacher; the Class 1 session opened with the whole class
  - Bulk, all-or-nothing and eligible-only check-in; five simultaneous check-ins for one student
    gave one visit and four refusals
  - Check-out with duration; follow-up refused during the school day and accepted after it
  - Roster snapshot; add and remove with reasons; finalize refused while anyone was unmarked
  - Correction with history; finalized session can't be cancelled; never-opened session
    cancelled; totals left the cancelled session out
  - Student history; CSV and PDF exports; another school's IDs return 404; a teacher gets 403
    everywhere; the session survived its timetable entry being deleted; ordinary attendance
    untouched
- The web app type-checks with no new errors. It has not been clicked through in a browser yet.

### Known behaviour to be aware of

- When a school first adds Library periods mid-term, every earlier Library period this
  term that nobody opened shows as **incomplete**. That follows B-10: nothing is marked absent,
  and they stay listed for follow-up. They can be cancelled, with a reason, or opened late (up
  to the end of the term).

