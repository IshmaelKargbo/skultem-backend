package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentScoreDTO;
import com.moriba.skultem.application.dto.GenerateReportCardsDTO;
import com.moriba.skultem.application.dto.GenerateReportCardsResultDTO;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.dto.ReportCardAssessmentScoreDTO;
import com.moriba.skultem.application.dto.ReportCardSubjectDTO;
import com.moriba.skultem.application.dto.ReportCardTermScoreDTO;
import com.moriba.skultem.application.dto.ReportCardSummaryDTO;
import com.moriba.skultem.application.dto.StudentDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.ReportCardMapper;
import com.moriba.skultem.domain.model.Assessment;
import com.moriba.skultem.domain.model.ReportCard;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.model.School;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ReportCardRepository;
import com.moriba.skultem.domain.repository.SchoolRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.domain.vo.FilterOperator;
import com.moriba.skultem.infrastructure.persistence.mapper.JsonMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GenerateReportCardsUseCase {

        private final ListEnrollmentByClassUseCase listEnrollmentByClassUseCase;
        private final GradeReportUseCase gradeReportUseCase;
        private final RankStudentUseCase rankStudentUseCase;
        private final AttendanceRepository attendanceRepo;
        private final ClassRepository classRepo;
        private final TermRepository termRepo;
        private final AssessmentRepository assessmentRepo;
        private final SchoolRepository schoolRepo;
        private final ReportCardRepository reportCardRepo;

        // One student's computed card, held until every student is done so positions can be ranked
        // across the whole class before anything is saved.
        private record Pending(StudentDTO student, List<ReportCardSubjectDTO> subjects, double average) {
        }

        public GenerateReportCardsResultDTO execute(String schoolId, String userId, GenerateReportCardsDTO param) {
                Term term;
                if (param.wholeYear() && param.academicYearId() != null && !param.academicYearId().isBlank()) {
                        // A whole-year card belongs to the year, not one term - it's filed under the year's last term.
                        term = termRepo.findByAcademicYearIdAndSchool(param.academicYearId(), schoolId).stream()
                                        .max(Comparator.comparingInt(Term::getTermNumber))
                                        .orElseThrow(() -> new NotFoundException("This academic year has no terms"));
                } else {
                        if (param.termId() == null || param.termId().isBlank()) {
                                throw new NotFoundException("Term not found");
                        }
                        term = termRepo.findByIdAndSchoolId(param.termId(), schoolId)
                                        .orElseThrow(() -> new NotFoundException("Term not found"));
                }

                var clazz = classRepo.findByIdAndSchool(param.classId(), schoolId)
                                .orElseThrow(() -> new NotFoundException("Class not found"));

                School school = schoolRepo.findById(schoolId).orElse(null);
                int passMark = clazz.getTemplate() != null ? clazz.getTemplate().getPassMark() : 50;

                // --- What this card covers ---------------------------------------------------------
                List<Term> terms = List.of(term);
                if (param.wholeYear()) {
                        terms = termRepo.findByAcademicYearIdAndSchool(term.getAcademicYear().getId(), schoolId)
                                        .stream()
                                        .sorted(Comparator.comparingInt(Term::getTermNumber))
                                        .toList();
                }

                var templateAssessments = clazz.getTemplate() == null ? List.<Assessment>of()
                                : assessmentRepo.findAllByTemplateIdAndSchoolId(clazz.getTemplate().getId(), schoolId)
                                                .stream()
                                                .sorted(Comparator.comparingInt(Assessment::getPosition))
                                                .toList();

                Set<String> selected = new LinkedHashSet<>();
                if (!param.wholeYear() && param.assessmentIds() != null) {
                        selected.addAll(param.assessmentIds());
                }
                // Picking every assessment is just the normal full-term card.
                var allIds = templateAssessments.stream().map(Assessment::getId).collect(Collectors.toSet());
                boolean subset = !selected.isEmpty() && !selected.containsAll(allIds);

                String scopeKey = param.wholeYear() ? "YEAR"
                                : subset ? selected.stream().sorted().collect(Collectors.joining(",")) : "ALL";
                String scopeLabel = param.wholeYear() ? "All terms"
                                : subset ? templateAssessments.stream().filter(e -> selected.contains(e.getId()))
                                                .map(Assessment::getName).collect(Collectors.joining(" + "))
                                                : null;
                if (subset && scopeLabel.isBlank()) {
                        throw new NotFoundException("None of the selected assessments belong to this class");
                }

                // One section/stream of the class (JSS 1 A vs JSS 1 B) when chosen, else the whole class.
                boolean narrowed = (param.sectionId() != null && !param.sectionId().isBlank())
                                || (param.streamId() != null && !param.streamId().isBlank());
                var roster = listEnrollmentByClassUseCase.execute(schoolId, param.classId(), param.streamId(),
                                param.sectionId(), term.getAcademicYear().getId(),
                                0, 0);

                int classSize = roster.getContent().size();

                LocalDate attendanceFrom = terms.stream().map(Term::getStartDate).min(LocalDate::compareTo)
                                .orElse(term.getStartDate());
                LocalDate attendanceTo = terms.stream().map(Term::getEndDate).max(LocalDate::compareTo)
                                .orElse(term.getEndDate());

                // --- Compute every student ---------------------------------------------------------
                List<Pending> pending = new ArrayList<>();

                for (StudentDTO student : roster.getContent()) {
                        // subject -> (term index -> rows of that term)
                        Map<String, Map<Integer, List<AssessmentScoreDTO>>> rowsBySubject = new LinkedHashMap<>();
                        Map<String, String> teacherBySubject = new LinkedHashMap<>();

                        for (int t = 0; t < terms.size(); t++) {
                                var termRows = gradeReportUseCase.execute(new ReportBuilderDTO(schoolId, "grades",
                                                List.of(
                                                                new Filter("studentAssessment.enrollment.student.id",
                                                                                FilterOperator.EQUALS, "select",
                                                                                student.id(), null, null),
                                                                new Filter("cycle.term.id", FilterOperator.EQUALS,
                                                                                "select", terms.get(t).getId(), null,
                                                                                null))),
                                                1, 500).getContent();

                                for (var row : termRows) {
                                        if (subset && !selected.contains(row.assessment()))
                                                continue;
                                        rowsBySubject.computeIfAbsent(row.subject(), k -> new LinkedHashMap<>())
                                                        .computeIfAbsent(t, k -> new ArrayList<>()).add(row);
                                        teacherBySubject.putIfAbsent(row.subject(), row.teacher());
                                }
                        }

                        if (rowsBySubject.isEmpty())
                                continue;

                        List<ReportCardSubjectDTO> subjects = new ArrayList<>();
                        for (var entry : rowsBySubject.entrySet()) {
                                List<Integer> termScores = new ArrayList<>();
                                List<ReportCardAssessmentScoreDTO> assessments = new ArrayList<>();
                                List<ReportCardTermScoreDTO> termScoreRows = new ArrayList<>();

                                for (var termEntry : entry.getValue().entrySet()) {
                                        int termIndex = termEntry.getKey();
                                        var rows = termEntry.getValue();

                                        double earned = rows.stream()
                                                        .mapToDouble(r -> r.weightScore() != null ? r.weightScore() : 0)
                                                        .sum();
                                        double possible = rows.stream()
                                                        .mapToDouble(r -> r.weight() != null ? r.weight() : 0).sum();
                                        // A chosen subset is marked out of just those assessments' weights, so
                                        // "First Test only" reads as a percentage, not as 20/100.
                                        double termScore = subset && possible > 0 ? earned / possible * 100 : earned;
                                        termScores.add((int) Math.round(termScore));
                                        termScoreRows.add(new ReportCardTermScoreDTO(terms.get(termIndex).getName(),
                                                        (int) Math.round(termScore)));

                                        String prefix = terms.size() > 1 ? terms.get(termIndex).getName() + " · " : "";
                                        // A whole-year card shows term totals and the final, not every assessment.
                                        if (param.wholeYear())
                                                continue;
                                        rows.stream()
                                                        .sorted(Comparator.comparingInt(r -> r.level() != null
                                                                        ? r.level() : Integer.MAX_VALUE))
                                                        .forEach(r -> assessments.add(new ReportCardAssessmentScoreDTO(
                                                                        prefix + r.name(), r.score(), r.weight(),
                                                                        termIndex * 100 + (r.level() != null
                                                                                        ? r.level() : 0))));
                                }

                                int subjectScore = (int) Math.round(termScores.stream().mapToInt(Integer::intValue)
                                                .average().orElse(0));
                                subjects.add(new ReportCardSubjectDTO(entry.getKey(),
                                                teacherBySubject.get(entry.getKey()), subjectScore, 100, subjectScore,
                                                resolveGrade(school, subjectScore), assessments,
                                                param.wholeYear() ? termScoreRows : null));
                        }

                        double average = subjects.stream().mapToInt(e -> e.score()).average().orElse(0);
                        pending.add(new Pending(student, subjects, average));
                }

                // --- Rank, then save -----------------------------------------------------------------
                List<ReportCardSummaryDTO> generated = new ArrayList<>();
                double totalAverage = 0;
                long passedCount = 0;

                for (Pending p : pending) {
                        var student = p.student();
                        double average = p.average();

                        int position = 0;
                        if (param.includeRanking()) {
                                // The whole-class, whole-term card keeps the existing term ranking. A partial,
                                // whole-year or single-section card is ranked among the students generated
                                // here, by their own average.
                                position = "ALL".equals(scopeKey) && !narrowed
                                                ? rankStudentUseCase.execute(student.id(), term.getId(), schoolId)
                                                : 1 + (int) pending.stream().filter(o -> o.average() > average)
                                                                .count();
                        }

                        String overallGrade = resolveGrade(school, average);
                        boolean passed = average >= passMark;

                        Double attendancePercentage = param.includeAttendance()
                                        ? computeAttendancePercentage(student.enrollmentId(), schoolId,
                                                        attendanceFrom, attendanceTo)
                                        : null;

                        var subjectsJson = JsonMapper.toJson(p.subjects());
                        var studentName = (student.givenNames() + " " + student.familyName()).trim();
                        var academicYearName = term.getAcademicYear() != null ? term.getAcademicYear().getName() : "";
                        var termName = param.wholeYear() ? "All terms" : term.getName();

                        var existing = reportCardRepo
                                        .findBySchoolIdAndStudentIdAndTermIdAndScopeKey(schoolId, student.id(),
                                                        term.getId(), scopeKey)
                                        .orElse(null);

                        ReportCard card;
                        if (existing != null) {
                                existing.regenerate(studentName, student.admissionNumber(), student.photo(),
                                                student.className(),
                                                classSize, termName, academicYearName, average, position,
                                                overallGrade, passed,
                                                attendancePercentage, subjectsJson, scopeLabel, userId);
                                card = existing;
                        } else {
                                card = ReportCard.generate(UUID.randomUUID().toString(), schoolId, student.id(),
                                                studentName,
                                                student.admissionNumber(), student.photo(), param.classId(),
                                                student.className(), classSize,
                                                term.getId(), termName, academicYearName, average, position,
                                                overallGrade, passed,
                                                attendancePercentage, null, subjectsJson, scopeKey, scopeLabel,
                                                userId);
                        }

                        reportCardRepo.save(card);
                        generated.add(ReportCardMapper.toSummaryDTO(card));

                        totalAverage += average;
                        if (passed)
                                passedCount++;
                }

                double classAverage = generated.isEmpty() ? 0 : totalAverage / generated.size();

                return new GenerateReportCardsResultDTO(generated.size(), passedCount, generated.size() - passedCount,
                                classAverage, generated);
        }

        private String resolveGrade(School school, double average) {
                if (school == null) {
                        return "N/A";
                }
                String grade = school.resolveGrade((int) Math.round(average));
                return grade != null ? grade : "N/A";
        }

        private Double computeAttendancePercentage(String enrollmentId, String schoolId, LocalDate start,
                        LocalDate end) {
                if (enrollmentId == null)
                        return null;

                var records = attendanceRepo.findByEnrollmentAndSchoolId(enrollmentId, schoolId, Pageable.unpaged())
                                .getContent().stream()
                                .filter(a -> !a.isHoliday())
                                .filter(a -> !a.getDate().isBefore(start) && !a.getDate().isAfter(end))
                                .toList();

                if (records.isEmpty())
                        return null;

                long present = records.stream().filter(a -> a.isPresent() || a.isLate()).count();
                return Math.round((present * 1000.0) / records.size()) / 10.0;
        }
}
