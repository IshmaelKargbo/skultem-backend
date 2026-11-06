package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ExamTimetableDTO;
import com.moriba.skultem.application.dto.ExamTimetableDTO.Missing;
import com.moriba.skultem.application.dto.ExamTimetableDTO.Paper;
import com.moriba.skultem.application.dto.ExamTimetableDTO.Ref;
import com.moriba.skultem.application.dto.ExamTimetableDTO.Summary;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.NotificationMapper;
import com.moriba.skultem.application.services.NotificationService;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Assessment;
import com.moriba.skultem.domain.model.ExamPaper;
import com.moriba.skultem.domain.model.ExamTimetable;
import com.moriba.skultem.domain.model.Notification;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.ExamPaperRepository;
import com.moriba.skultem.domain.repository.ExamTimetableRepository;
import com.moriba.skultem.domain.repository.NotificationRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.domain.vo.Priority;
import com.moriba.skultem.infrastructure.security.PermissionService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Exam timetables: one per assessment per term (Term 1 "1st Test", Term 1 "Exam" ...). The exam office sets one up,
// adds papers (one paper can be sat by several classes together), checks no class is missing a subject, then
// publishes it - only then do teachers and parents see it, and parents are told. A paper is refused when one of its
// classes or invigilators already has a paper at an overlapping time that day, on any timetable.
@Service
@Transactional
@RequiredArgsConstructor
public class ExamTimetableUseCase {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ExamTimetableRepository timetableRepo;
    private final ExamPaperRepository paperRepo;
    private final TermRepository termRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notificationRepo;
    private final NotificationService notificationService;
    private final SectionScopeService sectionScopeService;
    private final JdbcTemplate jdbc;

    public record TimetableInput(String termId, String assessmentId, String title, LocalDate startDate,
            LocalDate endDate, String instructions) {
    }

    public record PaperInput(String subjectId, String paper, LocalDate examDate, LocalTime startTime,
            LocalTime endTime, List<String> classIds, List<String> invigilatorIds, String venue, String notes) {
    }

    // The assessment a timetable belongs to, with its template (which decides the classes that sit it).
    private record AssessmentInfo(String id, String name, int position, String terms, String templateId,
            String templateName, int startsInTerm) {
    }

    private record ClassInfo(String id, String name, Level level) {
    }

    // ---- Reading

    /** Every assessment that runs in this term for classes the caller can see, with its timetable if set up. */
    public List<Summary> listForTerm(String schoolId, String termId, boolean canManage) {
        var term = term(schoolId, termId);
        var levels = sectionScopeService.levels();
        if (levels.isEmpty()) {
            return List.of();
        }
        var timetables = new HashMap<String, ExamTimetable>();
        for (var t : timetableRepo.findAllBySchoolAndTerm(schoolId, termId)) {
            timetables.put(t.getAssessmentId(), t);
        }
        var paperCounts = new HashMap<String, Integer>();
        jdbc.query("""
                SELECT p.timetable_id, count(*) AS n FROM exam_papers p
                JOIN exam_timetables t ON t.id = p.timetable_id
                WHERE t.school_id = ? AND t.term_id = ? GROUP BY p.timetable_id
                """, rs -> {
            paperCounts.put(rs.getString("timetable_id"), rs.getInt("n"));
        }, schoolId, termId);

        var out = new ArrayList<Summary>();
        for (var a : assessmentsFor(schoolId, levels)) {
            if (!runsIn(a, term)) {
                continue;
            }
            var t = timetables.get(a.id());
            if (!canManage && (t == null || !t.isPublished())) {
                continue;
            }
            out.add(new Summary(a.id(), a.name(), a.templateName(), a.position(), t == null ? null : t.getId(),
                    t == null ? null : t.getTitle(), t == null ? null : t.getStatus().name(),
                    t == null ? null : t.getStartDate(), t == null ? null : t.getEndDate(),
                    t == null ? 0 : paperCounts.getOrDefault(t.getId(), 0)));
        }
        return out;
    }

    /**
     * One timetable with its papers. classId narrows the papers to one class; invigilatorUserId to the papers that
     * person invigilates. Teachers only see a published timetable, and never the missing-papers check.
     */
    public ExamTimetableDTO get(String schoolId, String timetableId, String classId, String invigilatorUserId,
            boolean canManage) {
        var timetable = timetable(schoolId, timetableId);
        if (!canManage && !timetable.isPublished()) {
            throw new NotFoundException("Exam timetable not found");
        }
        var assessment = assessment(schoolId, timetable.getAssessmentId());
        var classes = classesFor(schoolId, assessment.templateId(), sectionScopeService.levels());
        requireSomeClassVisible(classes);
        return toDTO(schoolId, timetable, assessment, classes, classId, invigilatorUserId, canManage);
    }

    /** A parent's view: their child's class papers on every published timetable this academic year. */
    public List<ExamTimetableDTO> forStudent(String schoolId, String studentId) {
        var enrollment = enrollmentRepo.findTopByStudentAndSchoolIdOrderByCreatedAtDesc(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("This student isn't enrolled in a class"));
        String classId = enrollment.getClazz().getId();
        var clazz = new ClassInfo(classId, enrollment.getClazz().getName(), enrollment.getClazz().getLevel());

        var ids = jdbc.queryForList("""
                SELECT t.id FROM exam_timetables t
                JOIN terms tm ON tm.id = t.term_id
                JOIN academic_years ay ON ay.id = tm.academic_year_id
                WHERE t.school_id = ? AND t.status = 'PUBLISHED' AND ay.active = TRUE
                  AND EXISTS (SELECT 1 FROM exam_papers p JOIN exam_paper_classes pc ON pc.paper_id = p.id
                              WHERE p.timetable_id = t.id AND pc.class_id = ?)
                ORDER BY tm.term_number, t.start_date NULLS LAST
                """, String.class, schoolId, classId);

        var out = new ArrayList<ExamTimetableDTO>();
        for (var id : ids) {
            var timetable = timetable(schoolId, id);
            var assessment = assessment(schoolId, timetable.getAssessmentId());
            out.add(toDTO(schoolId, timetable, assessment, List.of(clazz), classId, null, false));
        }
        return out;
    }

    // ---- Timetables

    @AuditLogAnnotation(action = "EXAM_TIMETABLE_CREATED")
    public ExamTimetableDTO create(String schoolId, TimetableInput in) {
        if (in.termId() == null || in.assessmentId() == null) {
            throw new RuleException("Choose the term and the assessment this timetable is for");
        }
        var term = term(schoolId, in.termId());
        var assessment = assessment(schoolId, in.assessmentId());
        if (!runsIn(assessment, term)) {
            throw new RuleException(assessment.name() + " doesn't run in " + term.getName());
        }
        var classes = classesFor(schoolId, assessment.templateId(), sectionScopeService.levels());
        requireSomeClassVisible(classes);
        if (timetableRepo.findByTermAndAssessment(schoolId, in.termId(), in.assessmentId()).isPresent()) {
            throw new RuleException(term.getName() + " already has a timetable for " + assessment.name());
        }
        checkPeriod(term, in.startDate(), in.endDate());

        var timetable = ExamTimetable.create(schoolId, in.termId(), in.assessmentId(), in.title(), in.startDate(),
                in.endDate(), in.instructions());
        timetableRepo.save(timetable);
        return toDTO(schoolId, timetable, assessment, classes, null, null, true);
    }

    @AuditLogAnnotation(action = "EXAM_TIMETABLE_UPDATED")
    public ExamTimetableDTO update(String schoolId, String id, TimetableInput in) {
        var timetable = manageable(schoolId, id);
        var term = term(schoolId, timetable.getTermId());
        checkPeriod(term, in.startDate(), in.endDate());
        for (var p : paperRepo.findAllByTimetable(id)) {
            boolean outside = (in.startDate() != null && p.getExamDate().isBefore(in.startDate()))
                    || (in.endDate() != null && p.getExamDate().isAfter(in.endDate()));
            if (outside) {
                throw new RuleException("A paper is on " + p.getExamDate()
                        + ", outside that exam period. Move it first, or widen the period.");
            }
        }
        timetable.updateDetails(in.title(), in.startDate(), in.endDate(), in.instructions());
        timetableRepo.save(timetable);
        return get(schoolId, id, null, null, true);
    }

    @AuditLogAnnotation(action = "EXAM_TIMETABLE_PUBLISHED")
    public ExamTimetableDTO publish(String schoolId, String id) {
        var timetable = manageable(schoolId, id);
        var papers = paperRepo.findAllByTimetable(id);
        if (papers.isEmpty()) {
            throw new RuleException("Add at least one paper before publishing");
        }
        var user = PermissionService.getCurrentUser();
        String byName = userRepo.findById(user.userId()).map(u -> u.getName()).orElse(null);
        boolean republish = timetable.getPublishedAt() != null;
        timetable.publish(byName);
        timetableRepo.save(timetable);

        var classIds = new LinkedHashSet<String>();
        papers.forEach(p -> classIds.addAll(p.getClassIds()));
        notifyParents(schoolId, timetable, classIds, republish);
        return get(schoolId, id, null, null, true);
    }

    @AuditLogAnnotation(action = "EXAM_TIMETABLE_UNPUBLISHED")
    public ExamTimetableDTO unpublish(String schoolId, String id) {
        var timetable = manageable(schoolId, id);
        timetable.unpublish();
        timetableRepo.save(timetable);
        return get(schoolId, id, null, null, true);
    }

    @AuditLogAnnotation(action = "EXAM_TIMETABLE_DELETED")
    public void delete(String schoolId, String id) {
        var timetable = manageable(schoolId, id);
        if (timetable.isPublished()) {
            throw new RuleException("Unpublish the timetable before deleting it");
        }
        timetableRepo.delete(timetable);
    }

    // ---- Papers

    @AuditLogAnnotation(action = "EXAM_PAPER_CREATED")
    public ExamTimetableDTO addPaper(String schoolId, String timetableId, PaperInput in) {
        var timetable = manageable(schoolId, timetableId);
        var paper = ExamPaper.create(schoolId, timetableId, in.subjectId(), in.paper(), in.examDate(),
                in.startTime(), in.endTime(), in.venue(), in.notes(), in.classIds(), in.invigilatorIds());
        validatePaper(schoolId, timetable, paper);
        paperRepo.save(paper);
        return get(schoolId, timetableId, null, null, true);
    }

    @AuditLogAnnotation(action = "EXAM_PAPER_UPDATED")
    public ExamTimetableDTO updatePaper(String schoolId, String paperId, PaperInput in) {
        var paper = paperRepo.findByIdAndSchoolId(paperId, schoolId)
                .orElseThrow(() -> new NotFoundException("Exam paper not found"));
        var timetable = manageable(schoolId, paper.getTimetableId());
        requireClassesInScope(schoolId, paper.getClassIds());
        paper.update(in.subjectId(), in.paper(), in.examDate(), in.startTime(), in.endTime(), in.venue(), in.notes(),
                in.classIds(), in.invigilatorIds());
        validatePaper(schoolId, timetable, paper);
        paperRepo.save(paper);
        return get(schoolId, timetable.getId(), null, null, true);
    }

    @AuditLogAnnotation(action = "EXAM_PAPER_DELETED")
    public ExamTimetableDTO deletePaper(String schoolId, String paperId) {
        var paper = paperRepo.findByIdAndSchoolId(paperId, schoolId)
                .orElseThrow(() -> new NotFoundException("Exam paper not found"));
        var timetable = manageable(schoolId, paper.getTimetableId());
        requireClassesInScope(schoolId, paper.getClassIds());
        paperRepo.delete(paper);
        return get(schoolId, timetable.getId(), null, null, true);
    }

    private void validatePaper(String schoolId, ExamTimetable timetable, ExamPaper paper) {
        var assessment = assessment(schoolId, timetable.getAssessmentId());
        var term = term(schoolId, timetable.getTermId());

        if (paper.getExamDate().isBefore(term.getStartDate()) || paper.getExamDate().isAfter(term.getEndDate())) {
            throw new RuleException("The exam date has to fall within " + term.getName() + " ("
                    + term.getStartDate() + " to " + term.getEndDate() + ")");
        }
        if (!timetable.covers(paper.getExamDate())) {
            throw new RuleException("The exam date has to fall within the exam period ("
                    + timetable.getStartDate() + " to " + timetable.getEndDate() + ")");
        }
        if (names("SELECT id, name FROM subjects WHERE school_id = ? AND id = ?", schoolId, paper.getSubjectId())
                .isEmpty()) {
            throw new NotFoundException("Subject not found");
        }

        var classes = classMap(schoolId, paper.getClassIds());
        var scope = sectionScopeService.current();
        for (var id : paper.getClassIds()) {
            var c = classes.get(id);
            if (c == null) {
                throw new NotFoundException("Class not found");
            }
            if (!scope.allows(c.level())) {
                throw new RuleException(c.name() + " isn't in your section");
            }
        }
        var sitting = classesFor(schoolId, assessment.templateId(), null).stream().map(ClassInfo::id)
                .collect(java.util.stream.Collectors.toSet());
        for (var id : paper.getClassIds()) {
            if (!sitting.contains(id)) {
                throw new RuleException(classes.get(id).name() + " doesn't sit " + assessment.name()
                        + " (it uses a different assessment setup)");
            }
        }

        var teachers = teacherNames(schoolId, paper.getInvigilatorIds());
        for (var id : paper.getInvigilatorIds()) {
            if (!teachers.containsKey(id)) {
                throw new NotFoundException("Invigilator not found");
            }
        }

        for (var other : paperRepo.findAllBySchoolIdAndExamDate(schoolId, paper.getExamDate())) {
            if (other.getId().equals(paper.getId()) || !paper.overlaps(other)) {
                continue;
            }
            for (var classId : paper.getClassIds()) {
                if (other.getClassIds().contains(classId)) {
                    throw new RuleException(classes.get(classId).name() + " already sits "
                            + describe(schoolId, other) + " at that time");
                }
            }
            for (var teacherId : paper.getInvigilatorIds()) {
                if (other.getInvigilatorIds().contains(teacherId)) {
                    throw new RuleException(teachers.get(teacherId) + " is already invigilating "
                            + describe(schoolId, other) + " at that time");
                }
            }
        }
    }

    // ---- Notifications

    private void notifyParents(String schoolId, ExamTimetable timetable, Collection<String> classIds,
            boolean republish) {
        if (classIds.isEmpty()) {
            return;
        }
        String in = String.join(",", classIds.stream().map(c -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(schoolId);
        args.addAll(classIds);
        args.add(schoolId);
        args.addAll(classIds);
        var userIds = jdbc.queryForList("""
                SELECT DISTINCT p.user_id FROM parents p WHERE p.id IN (
                    SELECT s.parent_id FROM students s
                    JOIN enrollments e ON e.student_id = s.id
                    JOIN academic_years ay ON ay.id = e.academic_year_id AND ay.active = TRUE
                    WHERE e.school_id = ? AND e.status = 'ACTIVE' AND e.class_id IN (%s)
                    UNION
                    SELECT sp.parent_id FROM student_parents sp
                    JOIN enrollments e ON e.student_id = sp.student_id
                    JOIN academic_years ay ON ay.id = e.academic_year_id AND ay.active = TRUE
                    WHERE e.school_id = ? AND e.status = 'ACTIVE' AND sp.status = 'ACTIVE' AND e.class_id IN (%s))
                """.formatted(in, in), String.class, args.toArray());

        String title = (republish ? "Exam timetable updated: " : "Exam timetable published: ") + timetable.getTitle();
        String message = republish
                ? "The " + timetable.getTitle() + " timetable has changed. Please check your child's papers again."
                : "The " + timetable.getTitle() + " timetable is out. See when and where your child sits each paper.";
        Map<String, String> meta = Map.of("exam_timetable_id", timetable.getId(), "term_id", timetable.getTermId());

        for (var userId : userIds) {
            var owner = userRepo.findById(userId).orElse(null);
            if (owner == null) {
                continue;
            }
            var notification = Notification.create(UUID.randomUUID().toString(), schoolId, owner,
                    Notification.Type.ASSESSMENT, title, message, meta, Priority.NORMAL);
            notificationRepo.save(notification);
            long count = notificationRepo.countAllOpenByOwnerAndSchoolId(userId, schoolId);
            notificationService.sendToUser(userId, count, NotificationMapper.toDTO(notification));
        }
    }

    // ---- Building the DTO

    private ExamTimetableDTO toDTO(String schoolId, ExamTimetable timetable, AssessmentInfo assessment,
            List<ClassInfo> classes, String classId, String invigilatorUserId, boolean canManage) {
        var term = term(schoolId, timetable.getTermId());
        var visibleClassIds = classes.stream().map(ClassInfo::id).collect(java.util.stream.Collectors.toSet());

        String invigilatorTeacherId = invigilatorUserId == null ? null
                : jdbc.queryForList("SELECT id FROM teachers WHERE school_id = ? AND user_id = ?", String.class,
                        schoolId, invigilatorUserId).stream().findFirst().orElse("-");

        var papers = paperRepo.findAllByTimetable(timetable.getId()).stream()
                .filter(p -> p.getClassIds().stream().anyMatch(visibleClassIds::contains))
                .filter(p -> classId == null || classId.isBlank() || p.getClassIds().contains(classId))
                .filter(p -> invigilatorTeacherId == null || p.getInvigilatorIds().contains(invigilatorTeacherId))
                .sorted(Comparator.comparing(ExamPaper::getExamDate).thenComparing(ExamPaper::getStartTime))
                .toList();

        var allClassIds = new LinkedHashSet<String>();
        var teacherIds = new LinkedHashSet<String>();
        var subjectIds = new LinkedHashSet<String>();
        papers.forEach(p -> {
            allClassIds.addAll(p.getClassIds());
            teacherIds.addAll(p.getInvigilatorIds());
            subjectIds.add(p.getSubjectId());
        });
        var classNames = new HashMap<String, String>();
        classes.forEach(c -> classNames.put(c.id(), c.name()));
        classMap(schoolId, allClassIds).forEach((id, c) -> classNames.putIfAbsent(id, c.name()));
        var teachers = teacherNames(schoolId, teacherIds);
        var subjects = namesIn(schoolId, "subjects", subjectIds);

        var paperDTOs = papers.stream().map(p -> new Paper(p.getId(), p.getSubjectId(),
                subjects.getOrDefault(p.getSubjectId(), "Subject"), p.getPaper(), p.getExamDate(), p.getStartTime(),
                p.getEndTime(), p.getVenue(), p.getNotes(),
                p.getClassIds().stream().map(id -> new Ref(id, classNames.getOrDefault(id, "Class"))).toList(),
                p.getInvigilatorIds().stream().map(id -> new Ref(id, teachers.getOrDefault(id, "Teacher"))).toList()))
                .toList();

        List<Missing> missing = canManage && invigilatorUserId == null
                ? missingPapers(schoolId, timetable.getId(), classes) : null;

        return new ExamTimetableDTO(timetable.getId(), term.getId(), term.getName(), term.getStartDate(),
                term.getEndDate(), assessment.id(), assessment.name(), assessment.templateName(), timetable.getTitle(),
                timetable.getStartDate(), timetable.getEndDate(), timetable.getInstructions(),
                timetable.getStatus().name(), timetable.getPublishedAt(), timetable.getPublishedByName(),
                classes.stream().map(c -> new Ref(c.id(), c.name())).toList(), paperDTOs, missing);
    }

    // Each class's subjects (from its subject list) that no paper on this timetable covers yet.
    private List<Missing> missingPapers(String schoolId, String timetableId, List<ClassInfo> classes) {
        if (classes.isEmpty()) {
            return List.of();
        }
        String in = String.join(",", classes.stream().map(c -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(schoolId);
        classes.forEach(c -> args.add(c.id()));
        args.add(timetableId);
        var byClass = new LinkedHashMap<String, List<Ref>>();
        jdbc.query("""
                SELECT DISTINCT cs.class_id, s.id AS subject_id, s.name AS subject_name
                FROM class_subjects cs
                JOIN subjects s ON s.id = cs.subject_id
                WHERE cs.school_id = ? AND cs.class_id IN (%s)
                  AND NOT EXISTS (SELECT 1 FROM exam_papers p
                                  JOIN exam_paper_classes pc ON pc.paper_id = p.id
                                  WHERE p.timetable_id = ? AND p.subject_id = cs.subject_id
                                    AND pc.class_id = cs.class_id)
                ORDER BY s.name
                """.formatted(in), rs -> {
            byClass.computeIfAbsent(rs.getString("class_id"), k -> new ArrayList<>())
                    .add(new Ref(rs.getString("subject_id"), rs.getString("subject_name")));
        }, args.toArray());

        var out = new ArrayList<Missing>();
        for (var c : classes) {
            var subjects = byClass.get(c.id());
            if (subjects != null && !subjects.isEmpty()) {
                out.add(new Missing(c.id(), c.name(), subjects));
            }
        }
        return out;
    }

    // ---- Lookups

    private Term term(String schoolId, String termId) {
        return termRepo.findByIdAndSchoolId(termId, schoolId).orElseThrow(() -> new NotFoundException("Term not found"));
    }

    private ExamTimetable timetable(String schoolId, String id) {
        return timetableRepo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Exam timetable not found"));
    }

    // A timetable the caller may change: it has to cover at least one class in their sections.
    private ExamTimetable manageable(String schoolId, String id) {
        var timetable = timetable(schoolId, id);
        var assessment = assessment(schoolId, timetable.getAssessmentId());
        requireSomeClassVisible(classesFor(schoolId, assessment.templateId(), sectionScopeService.levels()));
        return timetable;
    }

    private void requireSomeClassVisible(List<ClassInfo> classes) {
        if (classes.isEmpty()) {
            throw new NotFoundException("Exam timetable not found");
        }
    }

    private void requireClassesInScope(String schoolId, Collection<String> classIds) {
        var scope = sectionScopeService.current();
        for (var c : classMap(schoolId, classIds).values()) {
            if (!scope.allows(c.level())) {
                throw new RuleException("This paper includes " + c.name() + ", which isn't in your section");
            }
        }
    }

    private static boolean runsIn(AssessmentInfo a, Term term) {
        var terms = Assessment.parseTerms(a.terms());
        return term.getTermNumber() >= a.startsInTerm() && (terms.isEmpty() || terms.contains(term.getTermNumber()));
    }

    private AssessmentInfo assessment(String schoolId, String assessmentId) {
        return jdbc.query("""
                SELECT a.id, a.name, a.position, a.terms, a.template_id, t.name AS template_name, t.starts_in_term
                FROM assessments a JOIN assessment_templates t ON t.id = a.template_id
                WHERE a.school_id = ? AND a.id = ?
                """, (rs, i) -> new AssessmentInfo(rs.getString("id"), rs.getString("name"), rs.getInt("position"),
                rs.getString("terms"), rs.getString("template_id"), rs.getString("template_name"),
                rs.getInt("starts_in_term")), schoolId, assessmentId)
                .stream().findFirst().orElseThrow(() -> new NotFoundException("Assessment not found"));
    }

    // Assessments of every template used by a class at these levels, template by template, in order.
    private List<AssessmentInfo> assessmentsFor(String schoolId, Collection<Level> levels) {
        String in = String.join(",", levels.stream().map(l -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(schoolId);
        args.add(schoolId);
        levels.forEach(l -> args.add(l.name()));
        return jdbc.query("""
                SELECT a.id, a.name, a.position, a.terms, a.template_id, t.name AS template_name, t.starts_in_term
                FROM assessments a JOIN assessment_templates t ON t.id = a.template_id
                WHERE a.school_id = ?
                  AND EXISTS (SELECT 1 FROM classes c WHERE c.assessment_template_id = a.template_id
                              AND c.school_id = ? AND c.status <> 'DELETED' AND c.level IN (%s))
                ORDER BY t.name, a.position
                """.formatted(in), (rs, i) -> new AssessmentInfo(rs.getString("id"), rs.getString("name"),
                rs.getInt("position"), rs.getString("terms"), rs.getString("template_id"),
                rs.getString("template_name"), rs.getInt("starts_in_term")), args.toArray());
    }

    // The classes that sit a template's assessments; levels null = every level.
    private List<ClassInfo> classesFor(String schoolId, String templateId, Collection<Level> levels) {
        if (levels != null && levels.isEmpty()) {
            return List.of();
        }
        var sql = new StringBuilder("""
                SELECT id, name, level FROM classes
                WHERE school_id = ? AND assessment_template_id = ? AND status <> 'DELETED'
                """);
        List<Object> args = new ArrayList<>(List.of(schoolId, templateId));
        if (levels != null) {
            sql.append(" AND level IN (").append(String.join(",", levels.stream().map(l -> "?").toList())).append(")");
            levels.forEach(l -> args.add(l.name()));
        }
        sql.append(" ORDER BY level_order, name");
        return jdbc.query(sql.toString(), (rs, i) -> new ClassInfo(rs.getString("id"), rs.getString("name"),
                Level.valueOf(rs.getString("level"))), args.toArray());
    }

    private Map<String, ClassInfo> classMap(String schoolId, Collection<String> ids) {
        var out = new HashMap<String, ClassInfo>();
        if (ids.isEmpty()) {
            return out;
        }
        List<Object> args = new ArrayList<>();
        args.add(schoolId);
        args.addAll(ids);
        jdbc.query("SELECT id, name, level FROM classes WHERE school_id = ? AND id IN ("
                + String.join(",", ids.stream().map(i -> "?").toList()) + ")", rs -> {
                    out.put(rs.getString("id"), new ClassInfo(rs.getString("id"), rs.getString("name"),
                            Level.valueOf(rs.getString("level"))));
                }, args.toArray());
        return out;
    }

    private Map<String, String> teacherNames(String schoolId, Set<String> ids) {
        var out = new HashMap<String, String>();
        if (ids.isEmpty()) {
            return out;
        }
        List<Object> args = new ArrayList<>();
        args.add(schoolId);
        args.addAll(ids);
        jdbc.query("""
                SELECT tc.id, trim(coalesce(u.given_name, '') || ' ' || coalesce(u.family_name, '')) AS name
                FROM teachers tc JOIN users u ON u.id = tc.user_id
                WHERE tc.school_id = ? AND tc.id IN (%s)
                """.formatted(String.join(",", ids.stream().map(i -> "?").toList())), rs -> {
            out.put(rs.getString("id"), rs.getString("name"));
        }, args.toArray());
        return out;
    }

    private Map<String, String> namesIn(String schoolId, String table, Set<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<Object> args = new ArrayList<>();
        args.add(schoolId);
        args.addAll(ids);
        return names("SELECT id, name FROM " + table + " WHERE school_id = ? AND id IN ("
                + String.join(",", ids.stream().map(i -> "?").toList()) + ")", args.toArray());
    }

    private Map<String, String> names(String sql, Object... args) {
        var out = new HashMap<String, String>();
        jdbc.query(sql, rs -> {
            out.put(rs.getString("id"), rs.getString("name"));
        }, args);
        return out;
    }

    private void checkPeriod(Term term, LocalDate start, LocalDate end) {
        if ((start != null && (start.isBefore(term.getStartDate()) || start.isAfter(term.getEndDate())))
                || (end != null && (end.isBefore(term.getStartDate()) || end.isAfter(term.getEndDate())))) {
            throw new RuleException("The exam period has to fall within " + term.getName() + " ("
                    + term.getStartDate() + " to " + term.getEndDate() + ")");
        }
    }

    private String describe(String schoolId, ExamPaper p) {
        var subject = names("SELECT id, name FROM subjects WHERE school_id = ? AND id = ?", schoolId,
                p.getSubjectId()).getOrDefault(p.getSubjectId(), "a paper");
        return subject + (p.getPaper() != null ? " (" + p.getPaper() + ")" : "") + ", "
                + TIME.format(p.getStartTime()) + "-" + TIME.format(p.getEndTime());
    }
}
