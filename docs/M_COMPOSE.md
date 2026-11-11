# M-Compose (document engine)

Skultem's server makes PDFs with M-Compose (`../../m-compose`). The browser never talks to the engine and never
sees its key. Each part is off until its settings are set, and while it is off the app keeps drawing PDFs itself.

## What it does

| Endpoint | Who | What |
|---|---|---|
| `GET /api/v1/m-compose/status` | anyone signed in | `{enabled, reportCards}`: which documents the server makes |
| `GET /api/v1/report-card/{id}/pdf` | as `GET /report-card/{id}` (staff, or the parent of an approved card) | one report card |
| `GET /api/v1/report-card/pdf?classId&termId[&scope]` | management, or the class's teacher | the class's cards for the term in one PDF, by student name; `scope` defaults to `ALL` (the whole term) |
| `GET /api/v1/m-compose/check` | system admins with `SETTINGS` | calls the engine: `{configured, reachable, keyAccepted, message, designer, reportCards, externalExams}` (the **Check connection** button on Platform Settings) |
| `GET /api/v1/m-compose/designer-link` | system admins with `SETTINGS` | `{enabled}`: is designer sign-in set up |
| `POST /api/v1/m-compose/designer-link` | system admins with `SETTINGS` | `{url}`: a one-time, 5-minute link that signs them in to the designer as a `designer` |

The designer is for platform staff only: Skultem has one M-Compose account, so its templates are the ones every school
prints. Errors from the engine come back as `502 DOCUMENT_SERVICE` with a message a person can act on.

## Settings (`.env`)

```bash
MCOMPOSE_URL=http://127.0.0.1:8000                     # the engine's API
MCOMPOSE_API_KEY=mck_...                               # a renderer key for the skultem account
MCOMPOSE_REPORT_CARD_TEMPLATE=sample:report-card-classic   # or a saved template's id
MCOMPOSE_DESIGNER_URL=http://skultem.localhost:3000    # Skultem's address on the designer
MCOMPOSE_SSO_SECRET=...                                # the account's sign-in secret
MCOMPOSE_TIMEOUT_SECONDS=60
MCOMPOSE_EXAM_RESULT_SLIP_TEMPLATE=sample:exam-result-slip            # the default
MCOMPOSE_EXAM_STATEMENT_TEMPLATE=sample:exam-statement-of-result      # the default
```

`status.enabled` only says the settings are filled in; **Check connection** (or `GET /m-compose/check`) actually calls
the engine and says whether it is running and accepts the key.

On the engine (`m-compose/api`, same `.env` as the server):

```bash
python -m app.accounts create-key skultem <name> renderer   # -> MCOMPOSE_API_KEY (shown once)
python -m app.accounts set-sso-secret skultem               # -> MCOMPOSE_SSO_SECRET (shown once; replaces any old one)
```

## Report card data

`ReportCardDocumentData` turns a card into the names a template binds. It covers everything the ready-made
`report-card-classic` and `report-card-modern` use (`student`, `class`, `term`, `period`, `results`, `total`,
`average`, `overall_grade`, `position`, `class_size`, `attendance`, `teacher_comment`, `principal_comment`, `teacher`,
`principal`, `grading_key`, `organisation`, `report_title`, `next_term`, `student_id`), plus `logo`, `photo`,
`teacher_signature`, `principal_signature` (data: URIs, a transparent pixel when missing), `school_motto`,
`school_address`, `school_phone`, `header_color`, `footer_note`, `academic_year`, `scope`, `passed`,
`subject_count`, `review_status` and `document_id`. Each row of `results` has `subject`, `teacher`, `score`,
`weight`, `weighted_score`, `grade`, `remarks` (from the school's remark scale) and `breakdown`
("Test 1: 18 · Exam: 70", or each term's score on a whole-year card). The school's report card settings
(position, attendance, remarks, photo, signatures) blank out what it does not print.

## External exams (NPSE, BECE, WASSCE)

The External Exams module (`/api/v1/external-exams`, app: its own **External Exams** menu) records official
results: the school adds an *exam class* (an exam, a class, a year, the exam's subjects); the class's students become
candidates; staff enter index numbers and scores; **Confirm** locks them; then each candidate's **result slip** or
**statement of result** (or every candidate's in one PDF) is shown in the page and printed. `ExternalExamDocumentData`
lists the names the designs use (`candidate`, `index_number`, `centre_number`, `exam_code`, `exam_name`, `exam_year`,
`results` rows of `subject`/`score`/`grade`/`points`/`remark`, `aggregate`, `aggregate_label`, `photo`, `logo`,
`principal`, `principal_signature`, ...). The ready-made designs are in m-compose's `samples/`; on an engine that
keeps its gallery in PostgreSQL run `python -m app.gallery sync` once so it has them. NPSE, BECE and WASSCE come
with starting grade scales each school should check on **Exams & grading**.

**Which design prints.** Report cards: the school's pick (Report Cards > Design), else
`MCOMPOSE_REPORT_CARD_TEMPLATE`. Exam slips and statements (External Exams > Print Designs): the exam's own design
(e.g. one for NPSE, another for BECE), else the school's general exam design, else
`MCOMPOSE_EXAM_RESULT_SLIP_TEMPLATE` / `MCOMPOSE_EXAM_STATEMENT_TEMPLATE`. Schools pick from the designs on the
engine: its ready-made ones and the templates saved in Skultem's account (`GET /api/v1/document-designs/{kind}`,
`GET|PUT /api/v1/external-exams/exams/{examId}/designs/{kind}`).

## Testing against a real engine

```bash
MCOMPOSE_TEST_URL=http://127.0.0.1:8000 MCOMPOSE_TEST_API_KEY=mck_... MCOMPOSE_TEST_OUT=/tmp \
  mvn test -Dtest=MComposeLiveTest
```
