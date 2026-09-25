package com.moriba.skultem.application.usecase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.moriba.skultem.application.dto.BulkStudentImportResultDTO;
import com.moriba.skultem.application.dto.BulkStudentImportResultDTO.SubjectChoice;
import com.moriba.skultem.application.dto.ClassSubjectResponse;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.application.dto.ParentRequest;
import com.moriba.skultem.application.dto.StudentRecord;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.Student.EnrollmentType;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.vo.Family;
import com.moriba.skultem.domain.vo.Gender;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BulkImportStudentsUseCase {

    public static final int MAX_ROWS = 2000;

    private static final String DEFAULT_NATIONALITY = "Sierra Leonean";
    private static final String DEFAULT_RELATIONSHIP = "Guardian";

    private static final Map<String, List<String>> COLUMNS = new LinkedHashMap<>();
    static {
        COLUMNS.put("firstName", List.of("firstname", "givenname", "givennames", "othernames"));
        COLUMNS.put("lastName", List.of("lastname", "familyname", "surname"));
        COLUMNS.put("gender", List.of("gender", "sex"));
        COLUMNS.put("dateOfBirth", List.of("dateofbirth", "dob", "birthdate", "birthday"));
        COLUMNS.put("class", List.of("class", "classname", "grade"));
        COLUMNS.put("guardianName", List.of("guardianname", "parentname", "guardian", "parent"));
        COLUMNS.put("guardianPhone", List.of("guardianphone", "parentphone", "guardianphonenumber",
                "parentphonenumber", "phone", "phonenumber"));
        COLUMNS.put("guardianEmail", List.of("guardianemail", "parentemail", "email"));
        COLUMNS.put("relationship", List.of("relationship", "guardianrelationship"));
        COLUMNS.put("admissionNumber", List.of("admissionnumber", "admissionno", "admno", "studentid"));
        COLUMNS.put("admissionDate", List.of("admissiondate", "dateofadmission"));
        COLUMNS.put("enrollmentType", List.of("enrollmenttype", "admissiontype", "type"));
        COLUMNS.put("subjects", List.of("subjects", "electives", "optionalsubjects"));
        COLUMNS.put("nationality", List.of("nationality"));
        COLUMNS.put("religion", List.of("religion"));
        COLUMNS.put("city", List.of("city", "town"));
        COLUMNS.put("address", List.of("address", "street", "homeaddress"));
        COLUMNS.put("previousSchool", List.of("previousschool", "lastschool"));
        COLUMNS.put("lastClass", List.of("lastclass", "previousclass"));
        COLUMNS.put("fatherName", List.of("fathername", "father"));
        COLUMNS.put("fatherOccupation", List.of("fatheroccupation", "fatherjob"));
        COLUMNS.put("fatherPhone", List.of("fatherphone", "fathercontact"));
        COLUMNS.put("motherName", List.of("mothername", "mother"));
        COLUMNS.put("motherOccupation", List.of("motheroccupation", "motherjob"));
        COLUMNS.put("motherPhone", List.of("motherphone", "mothercontact"));
    }

    private static final List<String> REQUIRED = List.of("firstName", "lastName", "gender", "dateOfBirth",
            "class", "guardianName", "guardianPhone", "fatherName", "fatherPhone", "fatherOccupation", "motherName",
            "motherOccupation", "motherPhone");

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT));

    private final SchoolRepository schoolRepo;
    private final AcademicYearRepository academicYearRepo;
    private final ClassSessionRepository sessionRepo;
    private final GetClassSubjectUseCase getClassSubjectUseCase;
    private final StudentRepository studentRepo;
    private final ParentRepository parentRepo;
    private final CreateStudentUseCase createStudentUseCase;
    private final PlatformTransactionManager transactionManager;
    private final SectionScopeService sectionScopeService;

    private record Planned(int row, StudentRecord record, BulkStudentImportResultDTO.Row summary) {
    }

    // A row whose subjects don't fit its class - carries the choices so the user can fix it.
    private static final class SubjectException extends RuleException {
        private final List<SubjectChoice> choices;

        SubjectException(String message, List<SubjectChoice> choices) {
            super(message);
            this.choices = choices;
        }
    }

    private record Checked(List<BulkStudentImportResultDTO.Row> results, List<Planned> planned, int failed) {
    }

    public BulkStudentImportResultDTO execute(String schoolId, InputStream csvInput, boolean dryRun) {
        var readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);
        Checked checked = readOnly.execute(status -> check(schoolId, csvInput));

        List<BulkStudentImportResultDTO.Row> results = new ArrayList<>(checked.results());
        List<Planned> planned = checked.planned();
        int failed = checked.failed();

        if (dryRun) {
            return new BulkStudentImportResultDTO(true, planned.size(), 0, failed, results);
        }

        int created = 0;
        for (var plan : planned) {
            var record = plan.record();
            BulkStudentImportResultDTO.Row outcome;
            try {
                if (record.parentId().isBlank()) {
                    String phone = record.parent().phone();
                    String existingId = readOnly.execute(status -> parentRepo.findByPhoneAndSchool(phone, schoolId)
                            .map(p -> p.getId()).orElse(null));
                    if (existingId != null) {
                        record = withParentId(record, existingId);
                    }
                }
                createStudentUseCase.execute(record);
                outcome = withOutcome(plan.summary(), "CREATED", null);
                created++;
            } catch (RuntimeException e) {
                outcome = withOutcome(plan.summary(), "FAILED", friendly(e));
                failed++;
            }
            results.set(results.indexOf(plan.summary()), outcome);
        }

        return new BulkStudentImportResultDTO(false, 0, created, failed, results);
    }

    private Checked check(String schoolId, InputStream csvInput) {
        var school = schoolRepo.findById(schoolId).orElseThrow(() -> new NotFoundException("school not found"));
        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new RuleException(
                        "No active academic year is set up - configure one before importing students."));

        // Someone limited to a management section can only import into classes of that section - a
        // row naming any other class simply isn't found, so it can't reach students outside their scope.
        var scope = sectionScopeService.currentOrAll();
        var sessions = sessionRepo
                .findBySchoolIdAndAcademicYearId(schoolId, academicYear.getId(), Pageable.unpaged())
                .getContent().stream()
                .filter(s -> scope.allows(s.getClazz().getLevel()))
                .toList();
        if (sessions.isEmpty()) {
            throw new RuleException("No classes are set up for the current academic year yet.");
        }

        Map<String, ClassSession> byComposedName = new HashMap<>();
        Map<String, List<ClassSession>> byClazzName = new HashMap<>();
        for (var session : sessions) {
            byComposedName.put(normalize(session.getName()), session);
            byClazzName.computeIfAbsent(normalize(session.getClazz().getName()), k -> new ArrayList<>()).add(session);
        }

        Map<String, ClassSubjectResponse> curriculumBySession = new HashMap<>();

        var parsed = parse(csvInput);
        if (parsed.records().isEmpty()) {
            throw new RuleException("The file has no student rows to import.");
        }
        if (parsed.records().size() > MAX_ROWS) {
            throw new RuleException("The file has " + parsed.records().size() + " rows - import at most "
                    + MAX_ROWS + " at a time by splitting it into smaller files.");
        }

        String schoolCity = school.getAddress() != null ? blankToNull(school.getAddress().city()) : null;
        String schoolStreet = school.getAddress() != null ? blankToNull(school.getAddress().street()) : null;

        var admissionNumbers = new AdmissionNumbers(schoolId);
        Set<String> phonesInFile = new HashSet<>();
        Set<String> studentsInFile = new HashSet<>();

        List<BulkStudentImportResultDTO.Row> results = new ArrayList<>();
        List<Planned> planned = new ArrayList<>();
        int failed = 0;
        int rowNum = 1; // row 1 is the header, like a spreadsheet shows it

        for (CSVRecord record : parsed.records()) {
            rowNum++;
            var cells = new Cells(record, parsed.columns());
            if (cells.allBlank()) {
                continue;
            }

            String name = (cells.get("firstName") + " " + cells.get("lastName")).trim();
            String className = cells.get("class");
            String phone = normalizePhone(cells.get("guardianPhone"));

            try {
                for (String column : REQUIRED) {
                    if (cells.get(column).isBlank()) {
                        throw new RuleException(label(column) + " is required.");
                    }
                }

                ClassSession session = resolveClass(className, byComposedName, byClazzName);
                Gender gender = parseGender(cells.get("gender"));
                LocalDate dob = parseDate(cells.get("dateOfBirth"), "Date of birth");
                if (!dob.isBefore(LocalDate.now())) {
                    throw new RuleException("Date of birth must be in the past.");
                }
                LocalDate admissionDate = cells.get("admissionDate").isBlank()
                        ? LocalDate.now()
                        : parseDate(cells.get("admissionDate"), "Admission date");
                EnrollmentType enrollmentType = parseEnrollmentType(cells.get("enrollmentType"));

                if (!studentsInFile.add(normalize(name) + "|" + dob)) {
                    throw new RuleException(name + " (born " + dob + ") appears more than once in this file.");
                }
                if (studentRepo.existsByNameAndDateOfBirth(cells.get("firstName"), cells.get("lastName"), dob,
                        schoolId)) {
                    throw new RuleException(name + " (born " + dob + ") is already a student here.");
                }

                if (phone.length() < 6) {
                    throw new RuleException("Guardian phone '" + cells.get("guardianPhone") + "' doesn't look valid.");
                }

                String admissionNumber = admissionNumbers.claim(cells.get("admissionNumber"));

                var curriculum = curriculumBySession.computeIfAbsent(session.getId(),
                        id -> curriculum(schoolId, session));
                List<String> electiveIds = resolveElectives(cells.get("subjects"), curriculum);

                String city = firstNonBlank(cells.get("city"), schoolCity);
                String street = firstNonBlank(cells.get("address"), schoolStreet);
                if (city == null || street == null) {
                    throw new RuleException("City and address are required - add them to the row, or to the"
                            + " school profile so they can be used as the default.");
                }
                
                var existingGuardian = parentRepo.findByPhoneAndSchool(phone, schoolId)
                        .or(() -> parentRepo.findByPhoneAndSchool(cells.get("guardianPhone"), schoolId));
                boolean reuseGuardian = existingGuardian.isPresent() || phonesInFile.contains(phone);
                phonesInFile.add(phone);

                String[] guardianName = splitName(cells.get("guardianName"));
                String guardianEmail = blankToNull(cells.get("guardianEmail"));
                var parentRequest = new ParentRequest(schoolId, guardianName[0], guardianName[1], guardianEmail,
                        phone, street, city);

                var family = new Family(blankToNull(cells.get("fatherName")), blankToNull(cells.get("motherName")),
                        blankToNull(cells.get("fatherOccupation")), blankToNull(cells.get("motherOccupation")),
                        blankToNull(normalizePhone(cells.get("motherPhone"))),
                        blankToNull(normalizePhone(cells.get("fatherPhone"))));

                var studentRecord = new StudentRecord(session.getId(), schoolId, null, cells.get("firstName"),
                        cells.get("lastName"), admissionNumber, admissionDate, enrollmentType,
                        blankToNull(cells.get("previousSchool")),
                        firstNonBlank(cells.get("nationality"), DEFAULT_NATIONALITY),
                        blankToNull(cells.get("religion")), city, street, blankToNull(cells.get("lastClass")),
                        null, gender, family, firstNonBlank(cells.get("relationship"), DEFAULT_RELATIONSHIP),
                        existingGuardian.map(p -> p.getId()).orElse(""), parentRequest, dob, electiveIds);

                var summary = new BulkStudentImportResultDTO.Row(rowNum, name, session.getName(), admissionNumber,
                        phone, reuseGuardian, "READY", null, null);
                planned.add(new Planned(rowNum, studentRecord, summary));
                results.add(summary);
            } catch (RuleException | NotFoundException e) {
                var choices = e instanceof SubjectException se ? se.choices : null;
                results.add(new BulkStudentImportResultDTO.Row(rowNum, name, className, cells.get("admissionNumber"),
                        phone, false, "FAILED", e.getMessage(), choices));
                failed++;
            }
        }

        return new Checked(results, planned, failed);
    }

    private final class AdmissionNumbers {
        private final String schoolId;
        private final Set<String> claimed = new HashSet<>();
        private final String prefix = "STU-" + Year.now().getValue() + "-";
        private int next = 1;

        AdmissionNumbers(String schoolId) {
            this.schoolId = schoolId;
        }

        String claim(String requested) {
            if (!requested.isBlank()) {
                String number = requested.trim().toUpperCase();
                if (!claimed.add(number)) {
                    throw new RuleException("Admission number " + number + " is used more than once in this file.");
                }
                if (studentRepo.existsByAdmissionNumberAndSchoolId(number, schoolId)) {
                    throw new RuleException("Admission number " + number + " already belongs to a student.");
                }
                return number;
            }

            while (true) {
                String candidate = prefix + String.format("%04d", next++);
                if (!claimed.contains(candidate)
                        && !studentRepo.existsByAdmissionNumberAndSchoolId(candidate, schoolId)) {
                    claimed.add(candidate);
                    return candidate;
                }
            }
        }
    }

    private record Parsed(Map<String, String> columns, List<CSVRecord> records) {
    }

    private record Cells(CSVRecord record, Map<String, String> columns) {
        String get(String column) {
            String header = columns.get(column);
            return header != null && record.isSet(header) ? record.get(header).trim() : "";
        }

        boolean allBlank() {
            for (String value : record) {
                if (value != null && !value.isBlank()) {
                    return false;
                }
            }
            return true;
        }
    }

    private Parsed parse(InputStream csvInput) {
        var format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setAllowMissingColumnNames(true)
                .setTrim(true)
                .build();

        try (var reader = new InputStreamReader(stripUtf8Bom(csvInput), StandardCharsets.UTF_8);
                CSVParser parser = format.parse(reader)) {

            Map<String, String> columns = new HashMap<>();
            for (String header : parser.getHeaderNames()) {
                String key = headerKey(header);
                COLUMNS.forEach((column, aliases) -> {
                    if (aliases.contains(key)) {
                        columns.putIfAbsent(column, header);
                    }
                });
            }

            List<String> missing = REQUIRED.stream().filter(c -> !columns.containsKey(c)).map(this::label).toList();
            if (!missing.isEmpty()) {
                throw new RuleException("The file is missing these columns: " + String.join(", ", missing)
                        + ". Download the template to see the expected headers.");
            }

            return new Parsed(columns, parser.getRecords());
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            throw new RuleException("Could not read the file - save it as CSV (comma separated) and try again.");
        }
    }

    private InputStream stripUtf8Bom(InputStream in) throws IOException {
        var pushback = new PushbackInputStream(in, 3);
        byte[] maybeBom = new byte[3];
        int read = pushback.read(maybeBom, 0, 3);
        boolean isBom = read == 3 && (maybeBom[0] & 0xFF) == 0xEF && (maybeBom[1] & 0xFF) == 0xBB
                && (maybeBom[2] & 0xFF) == 0xBF;
        if (!isBom && read > 0) {
            pushback.unread(maybeBom, 0, read);
        }
        return pushback;
    }

    private ClassSession resolveClass(String className, Map<String, ClassSession> byComposedName,
            Map<String, List<ClassSession>> byClazzName) {
        var exact = byComposedName.get(normalize(className));
        if (exact != null) {
            return exact;
        }
        var candidates = byClazzName.get(normalize(className));
        if (candidates == null || candidates.isEmpty()) {
            throw new RuleException("Class '" + className + "' not found for the current academic year.");
        }
        if (candidates.size() > 1) {
            throw new RuleException("'" + className + "' has " + candidates.size()
                    + " sections/streams - use the full name, e.g. '" + candidates.get(0).getName() + "'.");
        }
        return candidates.get(0);
    }

    // Same curriculum the enrol form shows. A class that can't be resolved (e.g. SSS without a
    // stream) has no choices, and CreateStudentUseCase reports the real problem on import.
    private ClassSubjectResponse curriculum(String schoolId, ClassSession session) {
        String streamId = session.getStream() != null ? session.getStream().getId() : null;
        try {
            return getClassSubjectUseCase.execute(schoolId, session.getClazz().getId(), streamId);
        } catch (RuleException | NotFoundException e) {
            return new ClassSubjectResponse(List.of(), List.of());
        }
    }

    // Matches subject names against the class's elective groups and checks each group's count,
    // the same rules CreateStudentUseCase enforces - so the dry run catches them too.
    private List<String> resolveElectives(String raw, ClassSubjectResponse curriculum) {
        var groups = curriculum.options();
        List<SubjectChoice> choices = groups.stream()
                .map(g -> new SubjectChoice(g.name(), g.select(),
                        g.list().stream().map(s -> s.name()).toList()))
                .toList();

        Map<String, Integer> pickedPerGroup = new HashMap<>();
        List<String> ids = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String subjectName : splitList(raw)) {
            if (!seen.add(normalize(subjectName))) {
                continue;
            }
            String id = null;
            for (var group : groups) {
                for (var subject : group.list()) {
                    if (normalize(subject.name()).equals(normalize(subjectName))) {
                        id = subject.id();
                        pickedPerGroup.merge(group.groupId(), 1, Integer::sum);
                    }
                }
            }
            if (id == null) {
                boolean isCore = curriculum.core().stream()
                        .anyMatch(s -> normalize(s.name()).equals(normalize(subjectName)));
                if (isCore) {
                    continue; // everyone takes it already - harmless to list
                }
                throw new SubjectException(groups.isEmpty()
                        ? "This class has no subjects to choose - leave the subjects column blank."
                        : "'" + subjectName + "' isn't a subject this class can choose.", choices);
            }
            ids.add(id);
        }

        for (var group : groups) {
            int picked = pickedPerGroup.getOrDefault(group.groupId(), 0);
            if (picked < group.select()) {
                throw new SubjectException("Pick " + group.select() + " subject" + (group.select() == 1 ? "" : "s")
                        + " for " + group.name() + (picked > 0 ? " (has " + picked + ")" : "") + ".", choices);
            }
        }
        return ids;
    }

    private Gender parseGender(String raw) {
        return switch (raw.trim().toUpperCase()) {
            case "M", "MALE", "BOY" -> Gender.MALE;
            case "F", "FEMALE", "GIRL" -> Gender.FEMALE;
            default -> throw new RuleException("Gender '" + raw + "' should be Male or Female (or M/F).");
        };
    }

    private LocalDate parseDate(String raw, String what) {
        for (var format : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw.trim(), format);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        throw new RuleException(what + " '" + raw + "' isn't a date - use day/month/year, e.g. 25/03/2014.");
    }

    private EnrollmentType parseEnrollmentType(String raw) {
        if (raw.isBlank()) {
            return EnrollmentType.EXISTING;
        }
        String key = raw.trim().toUpperCase().replaceAll("[^A-Z]", "");
        return switch (key) {
            case "NEW" -> EnrollmentType.NEW;
            case "TRANSFER", "TRANSFERRED" -> EnrollmentType.TRANSFER;
            case "REENROLLMENT", "REENROLMENT", "RETURNING" -> EnrollmentType.RE_ENROLLMENT;
            case "EXISTING", "CURRENT" -> EnrollmentType.EXISTING;
            default -> throw new RuleException(
                    "Enrollment type '" + raw + "' should be New, Transfer, Re-enrollment or Existing.");
        };
    }

    // --- small helpers
    // ----------------------------------------------------------------------

    private static BulkStudentImportResultDTO.Row withOutcome(BulkStudentImportResultDTO.Row row, String outcome,
            String message) {
        return new BulkStudentImportResultDTO.Row(row.row(), row.name(), row.className(), row.admissionNumber(),
                row.guardianPhone(), row.existingGuardian(), outcome, message, row.subjectChoices());
    }

    private static StudentRecord withParentId(StudentRecord r, String parentId) {
        return new StudentRecord(r.classId(), r.schoolId(), r.photo(), r.givenNames(), r.familyName(),
                r.admissionNumber(), r.admissionDate(), r.enrollmentType(), r.previousSchool(), r.nationality(),
                r.religion(), r.city(), r.street(), r.lastClass(), r.house(), r.gender(), r.family(),
                r.relationship(), parentId, r.parent(), r.dateOfBirth(), r.selectedOptionIds());
    }

    // CreateStudentUseCase's own messages are already written for a person ("You
    // must select at
    // least 2 subject(s) for Electives"); only point at the subjects column when
    // that's the fix.
    private static String friendly(RuntimeException e) {
        String message = e.getMessage() == null ? "Could not create this student." : e.getMessage();
        if (message.startsWith("You must select")) {
            return message + " - list them in the subjects column, separated by |.";
        }
        return message;
    }

    // "Mary Jane Kamara" -> given "Mary Jane", family "Kamara". A single word is
    // used for both
    // halves rather than leaving the family name empty.
    private static String[] splitName(String fullName) {
        String trimmed = fullName.trim().replaceAll("\\s+", " ");
        int lastSpace = trimmed.lastIndexOf(' ');
        return lastSpace < 0
                ? new String[] { trimmed, trimmed }
                : new String[] { trimmed.substring(0, lastSpace), trimmed.substring(lastSpace + 1) };
    }

    // Spaces, dashes, dots and brackets are formatting; a leading + is kept. Stored
    // the same way
    // so "076 123 456" and "076-123-456" are recognised as the same guardian.
    static String normalizePhone(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        String digits = trimmed.replaceAll("[^0-9]", "");
        return trimmed.startsWith("+") ? "+" + digits : digits;
    }

    private static List<String> splitList(String raw) {
        if (raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("[|;]")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private String label(String column) {
        return switch (column) {
            case "firstName" -> "First name";
            case "lastName" -> "Last name";
            case "gender" -> "Gender";
            case "dateOfBirth" -> "Date of birth";
            case "class" -> "Class";
            case "guardianName" -> "Guardian name";
            case "guardianPhone" -> "Guardian phone";
            default -> column;
        };
    }

    private static String headerKey(String header) {
        return header == null ? "" : header.replace("﻿", "").toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replace("﻿", "").trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
