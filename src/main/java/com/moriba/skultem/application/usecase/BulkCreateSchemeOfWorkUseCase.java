package com.moriba.skultem.application.usecase;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.BulkSchemeOfWorkResultDTO;
import com.moriba.skultem.application.dto.BulkSchemeRowResultDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.model.SchemeOfWork;
import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.SchemeOfWorkRepository;
import com.moriba.skultem.domain.repository.SubjectRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Bulk-creates Scheme of Work + Week entries from an uploaded CSV so an admin doesn't have to
// click through the one-at-a-time forms for every class/subject/term/week combination. Each row is
// one week; rows sharing the same class/subject/term reuse (or create once) the same scheme, then
// each gets its own week via CreateWeekUseCase - so a bad row (a typo, or a week number that
// already exists) is recorded and skipped, not fatal to the rest of the file.
@Service
@Transactional
@RequiredArgsConstructor
public class BulkCreateSchemeOfWorkUseCase {

    private static final List<String> REQUIRED_HEADERS = List.of("class", "subject", "term", "week", "topic");

    private final AcademicYearRepository academicYearRepo;
    private final ClassSessionRepository sessionRepo;
    private final SubjectRepository subjectRepo;
    private final TermRepository termRepo;
    private final SchemeOfWorkRepository schemeRepo;
    private final ManageSchemeOfWorkUseCase manageSchemeOfWorkUseCase;
    private final CreateWeekUseCase createWeekUseCase;

    public BulkSchemeOfWorkResultDTO execute(String schoolId, InputStream csvInput) {

        var academicYear = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new RuleException(
                        "No active academic year is set up - configure one before bulk-creating schemes."));

        var sessions = sessionRepo
                .findBySchoolIdAndAcademicYearId(schoolId, academicYear.getId(), Pageable.unpaged())
                .getContent();

        if (sessions.isEmpty()) {
            throw new RuleException("No classes are set up for the current academic year yet.");
        }

        var subjects = subjectRepo.findBySchool(schoolId, Pageable.unpaged()).getContent();
        var terms = termRepo.findByAcademicYearIdAndSchool(academicYear.getId(), schoolId);

        // "Nursery 1" resolves straight to its session when the class has only one
        // section/stream; otherwise the row needs the fully composed name (e.g. "SSS 1 A Art",
        // matching ClassSession.getName()) to say which one it means.
        Map<String, ClassSession> byComposedName = new HashMap<>();
        Map<String, List<ClassSession>> byClazzName = new HashMap<>();

        for (var session : sessions) {
            byComposedName.put(normalize(session.getName()), session);
            byClazzName.computeIfAbsent(normalize(session.getClazz().getName()), k -> new ArrayList<>()).add(session);
        }

        Map<String, Subject> subjectByName = new HashMap<>();
        for (var subject : subjects) {
            subjectByName.putIfAbsent(normalize(subject.getName()), subject);
        }

        Map<String, Term> termByName = new HashMap<>();
        for (var term : terms) {
            termByName.putIfAbsent(normalize(term.getName()), term);
        }

        List<CSVRecord> records = parse(csvInput);

        if (records.isEmpty()) {
            throw new RuleException("The file has no data rows to import.");
        }

        // Rows sharing the same class+subject+term reuse this scheme instead of each looking it
        // up (or trying to re-create it) independently.
        Map<String, SchemeOfWork> schemeByKey = new HashMap<>();

        List<BulkSchemeRowResultDTO> results = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        int failed = 0;

        // Row 1 is the header, so the first data record is row 2 - matches what a spreadsheet
        // app shows, making it easy to find/fix a flagged row in the original file.
        int rowNum = 1;

        for (CSVRecord record : records) {
            rowNum++;

            String className = safeGet(record, "class");
            String subjectName = safeGet(record, "subject");
            String termName = safeGet(record, "term");
            String weekRaw = safeGet(record, "week");
            String topic = safeGet(record, "topic");
            String subTopic = safeGet(record, "subtopic");
            String objectivesRaw = safeGet(record, "objectives");

            if (className.isBlank() && subjectName.isBlank() && termName.isBlank() && weekRaw.isBlank()
                    && topic.isBlank()) {
                continue;
            }

            Integer weekNumber = null;

            try {
                if (className.isBlank() || subjectName.isBlank() || termName.isBlank() || weekRaw.isBlank()
                        || topic.isBlank()) {
                    throw new RuleException("class, subject, term, week and topic are all required.");
                }

                try {
                    weekNumber = Integer.parseInt(weekRaw.trim());
                    if (weekNumber < 1) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    throw new RuleException("'" + weekRaw + "' is not a valid week number.");
                }

                ClassSession session = resolveClass(className, byComposedName, byClazzName);

                var subject = subjectByName.get(normalize(subjectName));
                if (subject == null) {
                    throw new RuleException("Subject '" + subjectName + "' not found.");
                }

                var term = termByName.get(normalize(termName));
                if (term == null) {
                    throw new RuleException("Term '" + termName + "' not found for the current academic year.");
                }

                String schemeKey = subject.getId() + "|" + term.getId() + "|" + session.getId();
                SchemeOfWork scheme = schemeByKey.computeIfAbsent(schemeKey,
                        k -> resolveOrCreateScheme(schoolId, subject, session, term));

                createWeekUseCase.execute(schoolId, scheme.getId(), weekNumber, topic,
                        subTopic.isBlank() ? null : subTopic, parseObjectives(objectivesRaw));

                results.add(new BulkSchemeRowResultDTO(rowNum, className, subjectName, termName, weekNumber, topic,
                        "CREATED", null));
                created++;
            } catch (RuleException | NotFoundException e) {
                boolean alreadyExists = e.getMessage() != null && e.getMessage().contains("week already exist");
                results.add(new BulkSchemeRowResultDTO(rowNum, className, subjectName, termName, weekNumber, topic,
                        alreadyExists ? "SKIPPED" : "FAILED",
                        alreadyExists ? "Week " + weekNumber + " already exists for this scheme." : e.getMessage()));
                if (alreadyExists) {
                    skipped++;
                } else {
                    failed++;
                }
            }
        }

        return new BulkSchemeOfWorkResultDTO(created, skipped, failed, results);
    }

    private SchemeOfWork resolveOrCreateScheme(String schoolId, Subject subject, ClassSession session, Term term) {
        return schemeRepo.findBySubjectAndTermAndSession(subject.getId(), term.getId(), session.getId(), schoolId)
                .orElseGet(() -> manageSchemeOfWorkUseCase.execute(schoolId, subject.getId(), session.getId(),
                        term.getId()));
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
            throw new RuleException("'" + className + "' matches " + candidates.size()
                    + " sections/streams - use the full name, e.g. '" + candidates.get(0).getName() + "'.");
        }

        return candidates.get(0);
    }

    private List<CSVRecord> parse(InputStream csvInput) {
        var format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setIgnoreSurroundingSpaces(true)
                .setTrim(true)
                .build();

        try (var reader = new InputStreamReader(stripUtf8Bom(csvInput), StandardCharsets.UTF_8);
                CSVParser parser = format.parse(reader)) {

            for (String required : REQUIRED_HEADERS) {
                if (parser.getHeaderNames().stream().noneMatch(h -> normalize(h).equals(required))) {
                    throw new RuleException(
                            "The file is missing a '" + required
                                    + "' column - expected headers: class, subject, term, week, topic, subTopic (optional), objectives (optional).");
                }
            }

            return parser.getRecords();
        } catch (IOException e) {
            throw new RuleException("Could not read the uploaded file - make sure it's a valid CSV.");
        }
    }

    // Excel's "CSV UTF-8" export (and some other spreadsheet tools) prefixes the file with a
    // UTF-8 byte-order-mark. Left in place, it silently glues itself onto the first header cell
    // ("class" becomes "﻿class"), which is invisible in a text editor but makes every lookup
    // for that column fail - exactly like the column wasn't there at all.
    private InputStream stripUtf8Bom(InputStream in) throws IOException {
        var pushback = new PushbackInputStream(in, 3);
        byte[] maybeBom = new byte[3];
        int read = pushback.read(maybeBom, 0, 3);

        boolean isUtf8Bom = read == 3
                && (maybeBom[0] & 0xFF) == 0xEF
                && (maybeBom[1] & 0xFF) == 0xBB
                && (maybeBom[2] & 0xFF) == 0xBF;

        if (!isUtf8Bom && read > 0) {
            pushback.unread(maybeBom, 0, read);
        }

        return pushback;
    }

    // isSet (not isMapped) - isMapped only checks the header exists at all, not that *this* row
    // actually has a value at that column. A row that simply ends early (e.g. no trailing comma
    // for a blank, optional trailing column like subTopic) is shorter than the header and throws
    // IndexOutOfBounds from CSVRecord.get() otherwise.
    private String safeGet(CSVRecord record, String header) {
        return record.isSet(header) ? record.get(header).trim() : "";
    }

    // Learning objectives are a list on Week, but a CSV cell is one string - multiple objectives
    // in one cell are "|"-separated (e.g. "Count to 10|Recognise numerals 1-10").
    private List<String> parseObjectives(String raw) {
        if (raw.isBlank()) {
            return List.of();
        }

        return Arrays.stream(raw.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    // Defensive: also strip a lingering U+FEFF character itself, in case a BOM survives as a
    // decoded character rather than raw bytes (e.g. a file that's already UTF-8 text by the time
    // it reaches here).
    private String normalize(String value) {
        return value == null ? "" : value.replace("﻿", "").trim().toLowerCase();
    }
}
