# Health Care — Business Requirements

Status: **Built (backend and web app), 2026-10-31.** The open questions in §13.2 were settled with their
proposed answers. What was built and how it was checked: §15.
Last updated: 2026-10-31 (renamed from "First Aid" to "Health Care").

The module was first built as **First Aid** and renamed to **Health Care** the same day, so it can grow beyond
sick bay visits (health profiles, incidents - §11). Its key is `health-care`, its pages and API live under
`/health-care`, and its tables are `health_care_*` (migration `V115__first_aid.sql` created them as `first_aid_*`;
`V116__rename_first_aid_to_health_care.sql` renamed them).

This document is the single source of requirements for the Health Care module. Any API notes added later
go into `FEATURES.md` and link back here, not repeat these rules. It follows the shape of
`LIBRARY_ATTENDANCE.md`, and reuses its patterns where they fit.

---

## 1. Purpose and scope

Health Care lets a school record **every time a student is seen in the sick bay**: when they arrived,
what was wrong, what was done for them, and how the visit ended (back to class, sent home, or referred
to a hospital or clinic). When a visit ends with the student sent home or referred, the student's
parents are told automatically.

Schools run their sick bays differently, so the policies that vary are **school settings** (§6), not
fixed rules: how often a student may visit, what must be recorded, whether medication may be given,
and when parents and the principal are told.

The first version covers **sick bay visits only**. Student health profiles (allergies, conditions,
regular medication), incident/injury reports and first aid stock are later work (§11).

### Core principles

1. **A child who needs care is never turned away by the system.** Settings can warn or ask for a
   reason, and can refuse routine repeat visits, but urgent complaints always go through (§6.2, L-4).
2. **Medical details are private.** Only the School Nurse and principal-level users see what was wrong
   and what was done. Nobody else sees clinical details: not teachers, not plain admins, not the
   activity feed, and not the audit log (§9).
3. **History is preserved.** A closed visit is never edited or deleted. Later information is added as
   a follow-up note, and a visit recorded by mistake is voided with a reason. It stays on record and
   is left out of reports.
4. **The system never invents data.** It never closes a visit by itself, never fills in a departure
   time, and never marks anyone absent.
5. **Health Care never writes to ordinary school attendance.** A student sent home keeps whatever
   attendance mark their teacher gave (§13.2, Q-4).
6. **Settings apply going forward.** Changing a setting never changes or re-judges past visits. A visit
   keeps a copy of any limit it was allowed past (§6.2, L-7).
7. Every operation is limited to the caller's school.

---

## 2. Existing system — verified dependencies

Checked in the source on 2026-10-31.

### 2.1 Modules and tenancy

- Modules are switched on per school with `@RequiresModule(FeatureModule.X)`. `FeatureModule` has no
  health care entry; a new `HEALTH_CARE` is needed (category `OPERATIONS`, not a starter module, no
  dependencies).
- The frontend maps routes to modules in `app/utils/modules.ts` (`ROUTE_MODULES`, `ROUTE_PATTERNS`).
  Menus and the route guard both use it.
- Controllers take the school from `@AuthenticationPrincipal(expression = "activeSchoolId")`.
  Repositories look records up by `…AndSchoolId`.
- Per-module school settings already exist as their own small tables and screens (e.g. library
  settings, `pages/library/settings.vue`; timetable settings). Health Care settings follow that pattern.

### 2.2 Roles

- `Role` enum: `OWNER, SYSTEM_ADMIN, PROPRIETOR, PRINCIPAL, SUPER_ADMIN, ADMIN, ACCOUNTANT, LIBRARIAN,
  TEACHER, PARENT`. There is no nurse role.
- Adding a role needs: the enum value, a migration that widens `school_users_role_check` (as
  `V111__librarian_role.sql` did for `LIBRARIAN`), and frontend entries in `utils/common.ts` (role enum,
  label, colour, icon, role picker options) and `components/me.vue`.
- `SUPER_ADMIN` passes any gate naming `ADMIN, OWNER, PROPRIETOR, PRINCIPAL, ACCOUNTANT, LIBRARIAN`
  (`PermissionService.SUPER_ADMIN_COVERS`). Whether `NURSE` joins that list is Q-1.
- The Librarian has a reduced portal: home shows the library dashboard (`pages/index.vue`), and the
  menu is laid out flat (`useMenu.ts`). The Nurse portal follows the same pattern.

### 2.3 Students, classes and the school calendar

- A student's current class comes from their `Enrollment` in the active academic year
  (`status ACTIVE` = currently enrolled). A class belongs to a school **level** (used for timings).
- Terms have start and end dates (`Term`). The time zone is `Africa/Freetown` for every school
  (`SchoolTimeZone.ZONE`).
- Parents are linked through `StudentParentRepository.findAllByStudentAndSchool`. A parent can only
  be notified if they have a user account (`parent.getUser() != null`). Parents without one are
  skipped (§7).

### 2.4 Notifications

- `Notification.create(id, schoolId, owner, type, title, message, meta, priority)`, saved with
  `NotificationRepository` and pushed live with `NotificationService.sendToUser`. Example:
  `MarkClassSessionAttendanceUseCase.notifyParentForAttendanceChange`.
- `Notification.Type` has no health value. A new `HEALTH` type needs the enum value and a migration
  that widens `notifications_type_check` (as `V43` did for `SUPPLY`).

### 2.5 Audit and activity

- `@AuditLogAnnotation(action = "…")` writes an `audit_logs` row through `AuditAspect`, **including
  the method's arguments and result as text** (up to 2000 characters). The annotation has only
  `action`; it can't leave the arguments out.
- `LogActivityUseCase` writes the human-readable activity feed. `ActivityType` has no health care
  value.

### 2.6 Patterns to reuse

- `@Idempotent(operation = "…")` for safe retries of create requests (used by library check-in).
- `ReportExportService` for CSV and PDF exports.
- The library's open visit / follow-up design (`LibraryVisit`) is the model for an open sick bay
  visit that nobody closed (§5.4).

---

## 3. Users

| User | Role |
|---|---|
| School Nurse (or whoever runs the sick bay) | `NURSE` (new) |
| Principal-level | `PRINCIPAL`, `OWNER`, `PROPRIETOR`, `SUPER_ADMIN` |
| Admin | `ADMIN`: see Q-2 |
| Teacher | `TEACHER`: no access in the first version (Q-3) |
| Parent | `PARENT`: notified as set in §6.5 and §7; no screens in the first version |

A school can give the `NURSE` role to any staff member who runs the sick bay. They don't need to be a
qualified nurse. The label in the app is "School Nurse".

---

## 4. Permissions

Every operation also requires `FeatureModule.HEALTH_CARE` and is limited to the caller's school.

| Capability | NURSE | PRINCIPAL / OWNER / PROPRIETOR | SUPER_ADMIN | ADMIN | TEACHER |
|---|---|---|---|---|---|
| Search students | ✅ | ✅ | ✅ | Q-2 | ❌ |
| Start, update and close a visit | ✅ | ❌ | Q-1 | ❌ | ❌ |
| Allow a visit past a limit (with reason) | ✅ | ❌ | Q-1 | ❌ | ❌ |
| Add a follow-up note to a closed visit | ✅ | ✅ | ✅ | ❌ | ❌ |
| Void a visit (with reason) | ✅ | ✅ | ✅ | ❌ | ❌ |
| Notify a parent by hand | ✅ | ❌ | Q-1 | ❌ | ❌ |
| See visits with clinical details | ✅ | ✅ | ✅ | Q-2 | ❌ |
| Reports and exports | ✅ | ✅ | ✅ | Q-2 | ❌ |
| Student profile "Health Care" tab | ✅ | ✅ | ✅ | Q-2 | ❌ |
| View Health Care settings | ✅ | ✅ | ✅ | ❌ | ❌ |
| Change Health Care settings (§6) | ❌ | ✅ | ✅ | ❌ | ❌ |

Rules:

- **P-1** Health Care has its own gate constants. It does not reuse any other module's gates.
- **P-2** Recording is nurse work. Principal-level users supervise: they view, add follow-up notes,
  void and report, but don't start or close visits.
- **P-3** Settings are school policy, so principal-level users set them. The nurse can see them (to
  know the rules) but can't change them (Q-8).
- **P-4** A request for a visit from another school behaves as "not found".
- **P-5** Health Care is not limited by management section in the first version, like the Library.
- **P-6** The `NURSE` role sees only the Health Care part of the portal, plus the shared pages every role
  has (notifications, profile, school notices).

---

## 5. Sick bay visits

### 5.1 Lifecycle

```
OPEN (in the sick bay) ──close──▶ CLOSED (with an outcome)
   │                                 │
   └──────────void──────────▶ VOIDED ◀──void──┘
```

- **OPEN**: the student is in the sick bay now.
- **CLOSED**: the visit ended with an outcome (§5.3). Locked: changes are follow-up notes only.
- **VOIDED**: recorded by mistake. Kept with who voided it, when and why. Not counted anywhere,
  including limits (§6.2).

### 5.2 Starting a visit

| Field | Required | Notes |
|---|---|---|
| Student | ✅ | Searched by name. Must be actively enrolled in the active academic year |
| Arrived at | ✅ | Defaults to now. Can be set earlier today (the nurse logs it after helping), never in the future or on another day |
| Complaint | ✅ | One or more from the school's complaint list (§6.6), plus optional free text. "Other" requires the free text |
| Brought by | Setting (§6.4) | Free text, e.g. "Class teacher, Mr Kamara" |
| Temperature (°C) | Setting (§6.4) | Number, 30.0–45.0 |
| Notes | — | Free text: what the nurse observed |

- **V-1 One open visit per student.** A student can't have two open visits. The database enforces it,
  as for library visits.
- **V-2 Safe retries.** Starting a visit uses `@Idempotent`, so a retry after a dropped connection
  doesn't create a second visit.
- **V-3 Repeat visits.** When a visit starts, the nurse always sees how many counted visits the
  student had this week and this term, with the last visit's date and complaint. Whether a repeat visit
  is allowed, warned about or refused is decided by the school's limit rules (§6.2).
- **V-4 Snapshot.** The student's name and class at the time are copied onto the visit, so history
  still reads correctly after a class change or exit.

### 5.3 While open, and closing

While a visit is open the nurse can update its complaint, temperature and notes, and record
**treatment**:

| Treatment field | Notes |
|---|---|
| Treatment given | Free text, e.g. "Cleaned and dressed graze on left knee". Required on close if set (§6.4) |
| Medication given | Zero or more lines: medicine name, dose, time given. Each line keeps who recorded it. Allowed only if the school permits medication (§6.3) |

Closing needs an **outcome**:

| Outcome | Extra fields | Parent notified (§7) |
|---|---|---|
| Returned to class | — | As set in §6.5 (default: no, the nurse can choose to) |
| Sent home | Collected by: name and relationship (required) | As set in §6.5 (default: **yes**) |
| Referred to hospital / clinic | Where to (required); ambulance called (yes/no); accompanied by (optional) | As set in §6.5 (default: **yes**) |

- **V-5** "Left at" defaults to now and can be set earlier, but not before "arrived at".
- **V-6** Closing is all-or-nothing and happens once. Two people closing the same visit at the same
  moment can't both succeed (row lock, as library check-out).
- **V-7** Time in the sick bay = left at − arrived at. Only closed visits have a duration.

### 5.4 Visits nobody closed

- An open visit becomes **needs follow-up** once the student's school day has ended (same end-of-day
  rule as library visits: the class level's timing, or midnight if there is none).
- Follow-up visits are listed separately on the nurse's dashboard. The nurse or a principal-level user
  closes them with an outcome and a **required reason**. They can't enter a departure time; the visit
  is recorded as "departure not recorded".
- Nothing is closed automatically. While a follow-up visit is still open it blocks a new visit for that
  student (V-1), so the nurse closes it first.

### 5.5 After closing

- **V-8 Follow-up notes.** The nurse or a principal-level user can add a note to a closed visit at any
  time. Notes are append-only and show who wrote them and when. Example: "Parent called: GP confirmed
  malaria, off school for 3 days."
- **V-9 Escalating.** If a student who returned to class is later sent home or referred for the same
  problem, the nurse **starts a new visit linked to the earlier one** ("continues visit of 10:15"). The
  earlier visit stays as it was. A linked continuation visit **does not count** towards limits (L-3).
- **V-10 Voiding.** A visit can be voided with a required reason. It's kept, shown struck through in
  lists, and left out of reports and limits. A voided visit can't be restored. If a parent was already
  notified, voiding does not send anything (the nurse contacts them directly).

---

## 6. School settings and limits

Each school sets its own Health Care policy on a **Health Care Settings** page. Every setting has a default
(below), so the module works the moment it's installed. A school that never opens the page gets
exactly the defaults.

### 6.1 How settings work

- **S-1 One settings record per school**, created with the defaults the first time it's read.
- **S-2 Changed by principal-level users only** (P-3). Every change is audited (who, when, old → new
  value) and shown on the settings page as "Last changed by … on …".
- **S-3 Forward only.** A change applies to visits started after it. Open visits keep the rules they
  started with, and closed visits are never re-judged (principle 6).
- **S-4 Enforced on the server.** The screens show the rules, but the API checks them, so they can't
  be bypassed.
- **S-5 Scoped rules.** Limit rules (§6.2) can apply to the whole school or to one or more school
  **levels** (e.g. a stricter rule for nursery than for senior secondary). When several rules apply to
  a student, each one is checked separately and the strictest result wins.

### 6.2 Visit limits (how often a student may visit)

A school can add any number of **limit rules**. Each rule says:

| Part | Choices | Example |
|---|---|---|
| Name | Free text | "One visit a week" |
| Applies to | Whole school, or chosen levels | Junior Secondary |
| Max visits | 1–50 | 1 |
| Per | **Day**, **Week** (Monday–Sunday), **Month**, or **Term** | Week |
| When reached | **Warn**, **Ask for a reason**, or **Refuse** | Ask for a reason |
| On | Yes / no (rules can be paused without deleting them) | Yes |

Default: **no rules.** Every visit is allowed, and the nurse still sees the visit counts (V-3).

What happens when a student has already had *Max visits* in the period and the nurse starts another:

| When reached | What the nurse sees | Can the visit start? |
|---|---|---|
| **Warn** | "Aminata has had 1 visit this week (limit 1). Last: Tue, headache." | Yes, straight away |
| **Ask for a reason** | The same message, plus a required reason box | Yes, once a reason is given |
| **Refuse** | "Aminata has reached this week's limit of 1 visit." | **No**, unless the complaint is urgent (L-4) |

Rules:

- **L-1 What counts.** Every visit that isn't voided counts, open or closed, whatever its outcome,
  including the first visit of a linked chain.
- **L-2 Periods are calendar periods** in the school's time zone: today; this Monday–Sunday; this
  calendar month; the term the visit date falls in. If the date isn't in any term, term rules don't
  apply.
- **L-3 Not counted:** voided visits (V-10), and continuation visits linked to an earlier visit
  (V-9). Following up the same problem is not a new visit.
- **L-4 Urgent complaints always go through.** Complaints marked **urgent** in the complaint list
  (§6.6) are never refused, whatever the rules say. If the visit is over a limit, the nurse sees the
  message, the visit starts, and it's flagged "urgent, over limit". By default **Breathing difficulty,
  Dizziness / fainting, Allergic reaction, Injury: cut or graze, Injury: sprain, bump or fall** and
  **Nosebleed** are urgent. Schools can mark more complaints urgent, but **can't un-mark these six**.
- **L-5 Refusing is recorded.** A refused attempt is saved as a **turned-away entry** (student, time,
  complaint, which rule) so the school can see how often its limit stops visits. It isn't a visit and
  doesn't count. The nurse can still give help that needs no record, and the screen says so: "If the
  student needs care, choose an urgent complaint or speak to the principal."
- **L-6 Order of checks.** One open visit per student (V-1) is checked first, then limits.
- **L-7 Over-limit visits keep a copy of the rule.** A visit allowed past a rule (warned, given a
  reason, or urgent) stores the rule's name, limit and period as they were, plus the reason and who
  gave it. Later changes to the rule don't change that record.
- **L-8 Reporting.** Over-limit visits and turned-away entries appear in reports (§10) and on the
  student's Health Care tab.

### 6.3 Medication

| Setting | Choices | Default |
|---|---|---|
| Medication in the sick bay | **Not given** / **Given and recorded** | Given and recorded |
| Same medicine again within | Off, or 1–24 hours | 4 hours |
| When that happens | **Warn** / **Ask for a reason** | Ask for a reason |
| Max medication lines per visit | Off, or 1–10 | Off |

- **M-1** With "Not given", the visit has no medication section, and the API refuses medication
  lines.
- **M-2** "Same medicine again within" compares the medicine name, case-insensitively, against the
  student's medication lines in any visit in the last N hours. It only warns or asks for a reason;
  medication is never refused by the system, because the nurse may have information the system doesn't.
- **M-3** Medicine names come from free text in the first version. A school medicine list is later
  work (with first aid stock).

### 6.4 What must be recorded

Each of these can be made required or optional:

| Field | Default |
|---|---|
| Brought by | Optional |
| Temperature on arrival | Optional |
| Treatment given (required when closing) | **Required** |
| Notes when the outcome is "Returned to class" | Optional |

Always required, whatever the settings: student, arrival time, complaint, outcome, and the outcome's own
fields (collected by, where referred to).

### 6.5 Who is told, and when

| Setting | Choices | Default |
|---|---|---|
| Notify parents automatically when the outcome is… | Any of: Returned to class, Sent home, Referred | Sent home, Referred |
| Let the nurse notify parents for other outcomes by hand | Yes / no | Yes |
| Tell the principal-level users when a student is referred to hospital | Yes / no | Yes |
| Tell the principal-level users when a student reaches N visits in a period | Off, or N per Week / Month / Term | Off |
| Alert the nurse when a student has been in the sick bay longer than | Off, or 10–240 minutes | 60 minutes |

- **N-0** Principal alerts are in-app notifications (type `HEALTH`) to every user with a
  principal-level role in the school. They say who and what happened ("Aminata was referred to
  Connaught Hospital"), with a link to the visit; no clinical notes.
- **N-0a** The long-stay alert is shown on the nurse's dashboard (the visit turns amber, with
  "In the sick bay for 1 h 10 min"). It is not a notification, and it never closes the visit.

### 6.6 Complaint list

- The school's complaint list starts as the standard list: Headache · Stomach ache · Fever / feeling
  hot · Vomiting · Diarrhoea · Cough / cold · Breathing difficulty · Dizziness / fainting · Injury:
  cut or graze · Injury: sprain, bump or fall · Nosebleed · Toothache · Eye problem · Skin rash /
  itching · Menstrual pain · Allergic reaction · Feeling unwell (general) · Other.
- **C-1** Schools can **add** complaints, **hide** standard ones they don't use, **rename** their own,
  and mark any complaint **urgent** (L-4). Standard complaints can be hidden but not renamed or deleted,
  so reports stay comparable across years.
- **C-2** The six urgent complaints in L-4 and "Other" can't be hidden.
- **C-3** A complaint used by any visit is never deleted, only hidden. Old visits keep showing it.

### 6.7 Defaults at a glance

A school that never changes anything gets: no visit limits; medication given and recorded, with a
4-hour same-medicine check that asks for a reason; treatment required when closing; parents told when
a child is sent home or referred; principals told about hospital referrals; a long-stay alert at
60 minutes; the standard complaint list.

---

## 7. Parent notifications

- **N-1** Closing a visit with an outcome the school chose in §6.5 notifies every linked parent with a
  user account, once per parent. Type `HEALTH`, priority `HIGH`.
- **N-2** For other outcomes, if the school allows it (§6.5), the nurse can tick "Notify parents" when
  closing, or press "Notify parents" on the closed visit later. Each visit notifies parents at most
  once by hand.
- **N-3** What parents see:
  - Title: "Aminata visited the sick bay" / "Aminata was sent home" / "Aminata was referred to
    hospital".
  - Message: date and time, the complaint, the outcome, and for referrals where to. **Not**
    temperature, nurse notes, medication or follow-up notes (Q-6).
- **N-4** If no linked parent has a user account, the visit closes normally. The nurse sees "No parent
  could be notified in the app. Contact them directly." The visit records that nobody was notified.
- **N-5** The visit records who was notified and when. It's shown on the visit.
- **N-6** Notifications are sent after the visit is saved. If sending fails, the visit still closes,
  and the nurse sees that notification failed and can retry with "Notify parents".

---

## 8. Screens

**Nurse portal** (role `NURSE`):

- **Home / dashboard**: students in the sick bay now (with time in, long stays in amber), visits
  needing follow-up, today's count by outcome, and a "Start visit" button.
- **Visits**: list with filters (date range, class, complaint, outcome, status, over limit) and search
  by student. Opens a visit's detail with its full record, treatment, medication, notifications, limit
  information and follow-up notes.
- **Reports** (§10).
- **Settings**: read-only view of the school's rules (P-3).

**Principal-level users**: Health Care in the main menu with Dashboard (read-only), Visits, Reports and
**Settings** (editable): limit rules, medication, required fields, notifications, complaint list.

**Student profile**: a "Health Care" tab listing the student's visits (date, complaint, outcome,
over-limit flag) and turned-away entries. Shown only when the module is installed and to roles allowed
in §4, like the Library tab.

---

## 9. Privacy

- **H-1** Clinical fields are complaint, temperature, notes, treatment, medication, outcome details,
  limit reasons and follow-up notes. Only the roles in §4 can read them through the API.
- **H-2** **The audit log must not contain clinical fields.** Health Care use cases must audit the visit
  ID, student ID and action only. This needs a new option on `@AuditLogAnnotation` (e.g.
  `includeDetails = false`) so `AuditAspect` skips arguments and result. That's an implementation
  prerequisite. Settings changes are not clinical and are audited in full (S-2).
- **H-3** The activity feed shows only "Health care visit" and the student's name, never the complaint or
  outcome.
- **H-4** Exports carry clinical details and are limited to the roles allowed to export (§4).
- **H-5** Nothing from Health Care appears in report cards, ID cards, ordinary attendance, or anything a
  teacher can see.

---

## 10. Reports

| Report | Content | Export |
|---|---|---|
| Daily log | Every visit on a date: time in/out, student, class, complaint, treatment, outcome | CSV, PDF |
| Visits by period | For a date range (max 92 days): counts by complaint, by outcome, and by class | CSV, PDF |
| Frequent visitors | Students with 3 or more visits in the range, with dates and complaints | CSV, PDF |
| Limits | For a date range: over-limit visits (by rule, with reasons) and turned-away entries | CSV, PDF |
| Student history | One student's visits, from the profile tab | CSV, PDF |
| Health trends | For a month (by week), a term (by month) or an academic year (by term): each complaint, visits, students seen, urgent, turned away, outcomes and classes per column; medicines given; busiest weekdays and hours. Visits between terms get an "Outside terms" column | CSV, PDF |

Voided visits are never counted. Open visits are counted as "still open", not under any outcome.

---

## 11. Scope

**First version**: `NURSE` role and portal, module switch, sick bay visits (§5), school settings and
limits (§6), parent and principal notifications (§6.5, §7), follow-up notes and voiding, student
profile tab, reports and exports, privacy rules (§9).

**Later** (not in the first version):

- Student health profile: allergies, conditions, blood group, regular medication, emergency contacts,
  with warnings when a visit starts.
- Incident / injury reports (where, how, witnesses), for accidents on school grounds.
- Health care stock and a school medicine list: supplies, use per visit, low-stock and expiry warnings.
- Parent consent for medication.
- Approval by a principal before a student is sent home.
- Parents seeing visit history in the parent portal.
- Staff (teacher) sick bay visits.
- SMS or email to parents (in-app notifications only for now).
- Limit rules per class or per student (the first version scopes them by level only).

---

## 12. Acceptance criteria

1. With the module off, no Health Care menu, page, tab or endpoint is available (endpoints answer as
   for any uninstalled module).
2. A user with only the `NURSE` role sees the Health Care portal and the shared pages, nothing else.
3. A nurse can start a visit for an enrolled student; a second open visit for that student is refused.
   Retrying the same request creates one visit.
4. Closing with an outcome chosen in §6.5 notifies each linked parent with an account exactly once. A
   parent without an account is skipped and the nurse is told.
5. A closed visit can't be edited; follow-up notes are added with author and time.
6. A voided visit keeps its data, shows as voided, and is missing from every report total and limit
   count.
7. A teacher or accountant gets "not found" / forbidden from every Health Care endpoint and sees no tab.
8. `audit_logs` rows for Health Care visit actions contain no complaint, notes, treatment or medication
   text.
9. An open visit from an earlier day shows under follow-up and blocks a new visit until closed with a
   reason.
10. All reads and writes are limited to the caller's school.
11. With a rule "1 per week, Refuse", a student's second non-urgent visit in the same Monday–Sunday is
    refused by the API and saved as a turned-away entry. The same visit with an urgent complaint starts
    and is flagged over limit. A continuation visit (V-9) and a visit after a voided one are not
    affected by the rule.
12. With "Ask for a reason", the API refuses the over-limit visit without a reason and accepts it with
    one; the reason and a copy of the rule are stored on the visit.
13. A rule applied to one level doesn't affect students in other levels. With two matching rules, the
    strictest result applies.
14. Changing or deleting a rule doesn't change any existing visit's record.
15. With medication "Not given", the API refuses medication lines.
16. A nurse can't change settings; a principal-level user can, and each change is audited with old and
    new values.
17. The six standard urgent complaints and "Other" can't be hidden or made non-urgent.

---

## 13. Decisions

### 13.1 Made (2026-10-31)

| # | Decision |
|---|---|
| D-1 | First version: sick bay visits only. Health profiles, incidents and stock come later. |
| D-2 | A new `NURSE` role ("School Nurse") with its own portal records visits. Principal-level users view and report. Teachers can't see medical details. |
| D-3 | Parents are notified automatically when a student is sent home or referred. The nurse can choose to notify for any other visit. (Now the defaults of §6.5.) |
| D-4 | Write this spec first, review, then build. |
| D-5 | Each school sets how often a student may visit the sick bay (e.g. "more than 1 in a week or not"), through limit rules (§6.2). Other policies that vary between schools are settings too (§6). |

### 13.2 Settled (2026-10-31): the proposed answers were taken

Building was asked for without changes to these, so each proposed answer below is the decision.

| # | Question | Proposed |
|---|---|---|
| Q-1 | Should `SUPER_ADMIN` get nurse powers automatically (start/close visits), as it does for Librarian? | **Yes**, add `NURSE` to `SUPER_ADMIN_COVERS`, for consistency. |
| Q-2 | What can a plain `ADMIN` see? | **Nothing clinical.** No Health Care access. Admins often handle general office work; medical details stay with the nurse and principal-level. |
| Q-3 | Should a class teacher see that their student is in the sick bay (no details)? | **Not in the first version.** Could be added later as "in sick bay since 10:15", with no complaint shown. |
| Q-4 | When a student is sent home, should ordinary attendance change? | **No.** Attendance stays the teacher's. The visit shows "sent home at 11:40". |
| Q-5 | ~~Should schools be able to edit the complaint list?~~ | Answered by D-5: **yes**, within C-1–C-3. |
| Q-6 | Should the parent's message include the treatment given (e.g. "given paracetamol 500mg")? | **No**, complaint and outcome only. The nurse calls the parent for details. |
| Q-7 | Should the nurse be able to backdate a visit to an earlier day (e.g. forgot to log yesterday)? | **No**, same day only. Late entries for another day are written as a follow-up note on a visit, or recorded on paper. |
| Q-8 | Should the nurse be able to change settings? | **No**, principal-level only (P-3). The nurse sees them read-only. |
| Q-9 | Should **Refuse** exist at all, or only Warn and Ask for a reason? | **Keep it**, with urgent complaints always allowed (L-4) and turned-away entries recorded (L-5). Some schools want a firm limit to stop students using the sick bay to skip lessons. |
| Q-10 | Should a week be Monday–Sunday, or the last 7 days? | **Monday–Sunday.** Staff and parents understand "this week". Rolling windows can be added later as another "Per" choice. |

---

## 14. Implementation outline (after review)

Backend, following Library Attendance:

- `Role.NURSE`; `FeatureModule.HEALTH_CARE`; `Notification.Type.HEALTH`; `ActivityType.HEALTH_CARE`.
- Migration `V115__first_aid.sql` (renamed by `V116`): widen `school_users_role_check` and `notifications_type_check`;
  tables `health_care_visits` (with a partial unique index for one open visit per student, the
  continuation link, and over-limit fields with the rule copy), `health_care_visit_medications`,
  `health_care_visit_notes`, `health_care_visit_notifications`, `health_care_turned_away`,
  `health_care_settings` (one row per school), `health_care_limit_rules` (+ `health_care_limit_rule_levels`),
  `health_care_complaints` (standard + school-added, with hidden and urgent flags).
- `AuditLogAnnotation.includeDetails` + `AuditAspect` support (H-2).
- Domain `HealthCareVisit` (lifecycle and rules §5), `HealthCarePolicy` (settings, limit evaluation as a
  pure function of the student's counted visits, the rules and the complaint: returns allow / warn /
  needs reason / refuse, so it is easy to test), use cases (start, update, add medication, close,
  resolve follow-up, add note, void, notify parents, settings, reports), `HealthCareController` with its
  own gates, exporter, and tests for every rule in §5–§7 and the checks in §12.

Frontend:

- Role entries (`utils/common.ts`, `me.vue`), nurse home on `pages/index.vue`, menu entries in
  `useMenu.ts`, `ModuleKey.HEALTH_CARE` and route mapping in `utils/modules.ts`.
- Pages under `pages/health-care/` (dashboard, visits, visit detail, reports, settings), the start/close
  visit dialogs (with the limit message and reason box), and `pages/students/[id]/health-care.vue`.

---

## 15. Implementation

### Where it lives

- Migrations: `V115__first_aid.sql` (role, notification and activity type checks; the tables, then named
  `first_aid_*`) and `V116__rename_first_aid_to_health_care.sql` (renamed to `health_care_settings`,
  `health_care_visits`, `health_care_turned_away`). Medication lines, follow-up notes, who was notified and the copy
  of a limit rule are JSON on the visit row; the complaint codes are also kept as `,CODE,` text for filtering.
- Domain: `HealthCareVisit`, `HealthCareSetting`, `HealthCareComplaint`, `HealthCareTurnedAway`, and the limit check
  `domain/service/HealthCareLimits` (a pure function, tested on its own).
- Application: `StartHealthCareVisitUseCase`, `UpdateHealthCareVisitUseCase`, `Add/RemoveHealthCareMedicationUseCase`,
  `CloseHealthCareVisitUseCase` (close and follow-up), `AddHealthCareNoteUseCase`, `VoidHealthCareVisitUseCase`,
  `NotifyHealthCareParentsUseCase`, `UpdateHealthCareSettingUseCase`; reads in `HealthCareService`; shared helpers
  (settings, student, school day, term, notifications) in `HealthCareSupport`.
- API: `HealthCareController` at `/api/v1/health-care`, exports in `HealthCareExporter`.
- Web app: `pages/health-care/` (dashboard, visits, visit, reports, settings), `components/health-care/`,
  `api/health-care.api.ts`, `utils/health-care.ts`, `useHealthCare`, `useHealthCareAccess`, and the student profile tab
  `pages/students/[id]/health-care.vue`. The nurse's home page is the Health Care dashboard; their menu is flat, like
  the librarian's.

### How the hard rules are enforced

- One open visit per student: partial unique index `uq_health_care_visits_one_open`, plus a clear message before
  any limit is checked (L-6).
- Limits are checked on the server when a visit starts. A refused visit returns a normal response with
  `turnedAway: true` (so the turned-away entry is saved), not an error.
- Closing, notes, voiding and medication lock the visit row, so two people acting at once can't both succeed.
- Audit (H-2): `@AuditLogAnnotation(includeDetails = false)` on every visit action. The audit row then names the
  method only. Settings changes keep full details (S-2).
- Starting a visit is `@Idempotent`, so a retry after a dropped connection makes one visit.

### Decisions made while building

- A continuation visit (V-9) must continue one of the same student's **closed visits from today**. This stops it
  being used to get round a limit across days.
- A complaint the school has hidden can't be chosen for a new visit, but stays on visits that already have it.
- The principal's visit-count alert is sent once, when a student **reaches** the number - not on every visit after.
- The nurse can open a student's profile but sees only its Health Care tab and no student actions.
- The same-medicine check compares names ignoring case and spaces at the ends. It never refuses (M-2).

### How it was checked (2026-10-31)

- Unit tests: `HealthCareLimitsTest` (9: periods, levels, strictest rule, urgent, paused rules, out of term),
  `HealthCareVisitTest` (8), `HealthCareSettingTest` (4), `StartHealthCareVisitUseCaseTest` (6: refuse records
  turned-away, urgent goes through, reason required, continuation skips limits, already open, hidden
  complaint), `AuditAspectDetailsTest` (2: no medical text in audit rows). All pass.
- All 115 migrations applied in order to an empty Postgres 17 database; the three new tables' columns match the
  JPA entities exactly.
- Web app type check: no errors in the Health Care files.
- **Not yet done:** running the app end to end (starting visits, closing, notifications reaching a parent's
  account) against a real database.

### Known behaviour to be aware of

- A plain `ADMIN` has no Health Care access at all (Q-2), including the menu and student tab.
- Notifications are in-app only. Parents without an account are listed as "no parent could be notified".
