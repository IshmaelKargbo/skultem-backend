#!/usr/bin/env bash
set -euo pipefail

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required. Install jq and retry." >&2
  exit 1
fi

BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
USER_ID="${USER_ID:-USR-2026-0001}"
SCHOOL_ID="${SCHOOL_ID:-SCL-2026-0001}"

STAMP="$(date +%Y%m%d%H%M%S)"
LEVEL_ORDER="$((10#$(date +%M%S) + 1000))"

api() {
  local method="$1"
  local path="$2"
  local body="${3:-}"

  if [[ -n "$body" ]]; then
    curl -sS -X "$method" "${BASE_URL}${path}" \
      -H "Content-Type: application/json" \
      -H "X-User-Id: ${USER_ID}" \
      -H "X-School-Id: ${SCHOOL_ID}" \
      -d "$body"
  else
    curl -sS -X "$method" "${BASE_URL}${path}" \
      -H "X-User-Id: ${USER_ID}" \
      -H "X-School-Id: ${SCHOOL_ID}"
  fi
}

extract_data() {
  local response="$1"
  local message
  message="$(printf '%s' "$response" | jq -r '.message // empty')"
  if [[ "$(printf '%s' "$response" | jq -r '.status // empty')" != "success" ]]; then
    echo "API call failed: ${message:-unknown error}" >&2
    printf '%s\n' "$response" >&2
    exit 1
  fi
  printf '%s' "$response" | jq -c '.data'
}

echo "[1/10] Ensure an active academic year exists..."
AY_CREATE_RES="$(api POST /academic_year "{\"name\":\"Seed AY ${STAMP}\",\"startDate\":\"2026-09-01\",\"endDate\":\"2027-07-31\"}")"
AY_ID="$(extract_data "$AY_CREATE_RES" | jq -r '.id')"
api PUT "/academic_year/${AY_ID}" >/dev/null

echo "[2/10] Create term..."
api POST /term "{\"name\":\"Seed Term ${STAMP}\",\"startDate\":\"2026-09-01\",\"endDate\":\"2026-12-20\"}" >/dev/null

echo "[3/10] Create section..."
SECTION_RES="$(api POST /section "{\"name\":\"Seed Section ${STAMP}\",\"description\":\"Seed section for enrollment flow\"}")"
SECTION_ID="$(extract_data "$SECTION_RES" | jq -r '.id')"

echo "[4/10] Create subjects..."
SUBJECT_CORE_A="$(extract_data "$(api POST /subject "{\"name\":\"Seed Mathematics ${STAMP}\",\"code\":\"SMA${STAMP: -4}\",\"description\":\"Core subject\"}")" | jq -r '.id')"
SUBJECT_CORE_B="$(extract_data "$(api POST /subject "{\"name\":\"Seed English ${STAMP}\",\"code\":\"SEN${STAMP: -4}\",\"description\":\"Core subject\"}")" | jq -r '.id')"
SUBJECT_OPT="$(extract_data "$(api POST /subject "{\"name\":\"Seed Commerce ${STAMP}\",\"code\":\"SCM${STAMP: -4}\",\"description\":\"Optional subject\"}")" | jq -r '.id')"

echo "[5/10] Create assessment template + assignments..."
TEMPLATE_RES="$(api POST /assessment/template "{\"name\":\"Seed Template ${STAMP}\",\"description\":\"Template for seeded class\"}")"
TEMPLATE_ID="$(extract_data "$TEMPLATE_RES" | jq -r '.id')"
api POST "/assessment/template/${TEMPLATE_ID}/assignment" "{\"assignments\":[{\"name\":\"Test 1\",\"weight\":40},{\"name\":\"Exam\",\"weight\":60}]}" >/dev/null

echo "[6/10] Create class with assessment template..."
CLASS_RES="$(api POST /class "{\"name\":\"Seed JSS ${STAMP}\",\"level\":\"JSS\",\"levelOrder\":${LEVEL_ORDER},\"sections\":[\"${SECTION_ID}\"],\"streams\":[],\"assessmentTemplateId\":\"${TEMPLATE_ID}\"}")"
CLASS_ID="$(extract_data "$CLASS_RES" | jq -r '.id')"

echo "[7/10] Create optional subject group for class..."
GROUP_RES="$(api POST /subject-group "{\"name\":\"Seed Optional Group ${STAMP}\",\"classId\":\"${CLASS_ID}\",\"streamId\":null,\"totalSelection\":1}")"
GROUP_ID="$(extract_data "$GROUP_RES" | jq -r '.id')"

echo "[8/10] Assign class subjects (2 core + 1 optional)..."
api POST "/assignment/class/${CLASS_ID}" "{\"assignments\":[{\"subjectId\":\"${SUBJECT_CORE_A}\",\"mandatory\":true},{\"subjectId\":\"${SUBJECT_CORE_B}\",\"mandatory\":true},{\"subjectId\":\"${SUBJECT_OPT}\",\"groupId\":\"${GROUP_ID}\",\"mandatory\":false}]}" >/dev/null

echo "[9/10] Resolve class session id..."
SESSIONS_RES="$(api GET "/class/session?page=1&size=200")"
SESSION_ID="$(extract_data "$SESSIONS_RES" | jq -r --arg classId "$CLASS_ID" --arg sectionId "$SECTION_ID" '.[] | select(.clazzId==$classId and .sectionId==$sectionId) | .id' | head -n1)"
if [[ -z "$SESSION_ID" ]]; then
  echo "Unable to resolve class session for class ${CLASS_ID} and section ${SECTION_ID}" >&2
  exit 1
fi

echo "[10/10] Create student and enroll with optional subject selection..."
STUDENT_RES="$(api POST /student "{\"givenNames\":\"Seed\",\"familyName\":\"Student ${STAMP}\",\"academicNumber\":\"AC-${STAMP}\",\"gender\":\"FEMALE\",\"dateOfBirth\":\"2011-05-18\",\"classSessionId\":\"${SESSION_ID}\",\"selectedOptionIds\":[\"${SUBJECT_OPT}\"]}")"
STUDENT_ID="$(extract_data "$STUDENT_RES" | jq -r '.id')"

echo
printf 'Seed completed:\n'
printf 'School ID: %s\n' "$SCHOOL_ID"
printf 'Academic Year ID: %s\n' "$AY_ID"
printf 'Section ID: %s\n' "$SECTION_ID"
printf 'Template ID: %s\n' "$TEMPLATE_ID"
printf 'Class ID: %s\n' "$CLASS_ID"
printf 'Group ID: %s\n' "$GROUP_ID"
printf 'Class Session ID: %s\n' "$SESSION_ID"
printf 'Student ID: %s\n' "$STUDENT_ID"
