package com.moriba.skultem.application.usecase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.ExamScheduleDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.ExamSchedule;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ExamScheduleRepository;
import com.moriba.skultem.domain.repository.SubjectRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// The exam timetable: list, add, change and remove papers. A paper is refused when its class already sits
// another paper at an overlapping time that day, or its invigilator is already watching one.
@Service
@Transactional
@RequiredArgsConstructor
public class ExamScheduleUseCase {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ExamScheduleRepository repo;
    private final TermRepository termRepo;
    private final ClassRepository classRepo;
    private final SubjectRepository subjectRepo;
    private final TeacherRepository teacherRepo;
    private final SectionScopeService sectionScopeService;
    private final JdbcTemplate jdbc;

    public record Input(String termId, String classId, String subjectId, String paper, LocalDate examDate,
            LocalTime startTime, LocalTime endTime, String venue, String invigilatorId, String notes) {
    }

    // termId is required; classId narrows to one class; invigilatorUserId to one invigilator's papers ("mine").
    public List<ExamScheduleDTO> list(String schoolId, String termId, String classId, String invigilatorUserId) {
        var levels = sectionScopeService.levels();
        if (levels.isEmpty()) {
            return List.of();
        }
        var sql = new StringBuilder("""
                SELECT e.id, e.term_id, t.name AS term_name, e.class_id, c.name AS class_name, c.level,
                       e.subject_id, s.name AS subject_name, e.paper, e.exam_date, e.start_time, e.end_time,
                       e.venue, e.invigilator_id,
                       trim(coalesce(u.given_name, '') || ' ' || coalesce(u.family_name, '')) AS invigilator_name,
                       e.notes
                FROM exam_schedules e
                JOIN terms t ON t.id = e.term_id
                JOIN classes c ON c.id = e.class_id
                JOIN subjects s ON s.id = e.subject_id
                LEFT JOIN teachers tc ON tc.id = e.invigilator_id
                LEFT JOIN users u ON u.id = tc.user_id
                WHERE e.school_id = ? AND e.term_id = ?
                """);
        List<Object> args = new ArrayList<>(List.of(schoolId, termId));
        if (classId != null && !classId.isBlank()) {
            sql.append(" AND e.class_id = ?");
            args.add(classId);
        }
        if (invigilatorUserId != null) {
            sql.append(" AND tc.user_id = ?");
            args.add(invigilatorUserId);
        }
        sql.append(" AND c.level IN (").append(String.join(",", levels.stream().map(l -> "?").toList())).append(")");
        levels.forEach(l -> args.add(l.name()));
        sql.append(" ORDER BY e.exam_date, e.start_time, c.level_order, c.name, s.name");
        return jdbc.query(sql.toString(), this::toDTO, args.toArray());
    }

    @AuditLogAnnotation(action = "EXAM_SCHEDULE_CREATED")
    public ExamScheduleDTO create(String schoolId, Input in) {
        validate(schoolId, in, null);
        var schedule = ExamSchedule.create(UUID.randomUUID().toString(), schoolId, in.termId(), in.classId(),
                in.subjectId(), in.paper(), in.examDate(), in.startTime(), in.endTime(), in.venue(),
                in.invigilatorId(), in.notes());
        repo.save(schedule);
        return get(schoolId, schedule.getId());
    }

    @AuditLogAnnotation(action = "EXAM_SCHEDULE_UPDATED")
    public ExamScheduleDTO update(String schoolId, String id, Input in) {
        var schedule = find(schoolId, id);
        validate(schoolId, in, id);
        schedule.update(in.termId(), in.classId(), in.subjectId(), in.paper(), in.examDate(), in.startTime(),
                in.endTime(), in.venue(), in.invigilatorId(), in.notes());
        repo.save(schedule);
        return get(schoolId, id);
    }

    @AuditLogAnnotation(action = "EXAM_SCHEDULE_DELETED")
    public void delete(String schoolId, String id) {
        repo.delete(find(schoolId, id));
    }

    private ExamSchedule find(String schoolId, String id) {
        var schedule = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Exam paper not found"));
        // A section-limited exam officer can only touch papers in their own sections.
        var clazz = classRepo.findByIdAndSchool(schedule.getClassId(), schoolId).orElse(null);
        if (clazz != null && !sectionScopeService.current().allows(clazz.getLevel())) {
            throw new NotFoundException("Exam paper not found");
        }
        return schedule;
    }

    private ExamScheduleDTO get(String schoolId, String id) {
        var schedule = repo.findByIdAndSchoolId(id, schoolId).orElseThrow();
        return list(schoolId, schedule.getTermId(), schedule.getClassId(), null).stream()
                .filter(e -> e.id().equals(id)).findFirst()
                .orElseThrow(() -> new NotFoundException("Exam paper not found"));
    }

    private void validate(String schoolId, Input in, String selfId) {
        if (!in.endTime().isAfter(in.startTime())) {
            throw new RuleException("The paper has to end after it starts");
        }
        var term = termRepo.findByIdAndSchoolId(in.termId(), schoolId)
                .orElseThrow(() -> new NotFoundException("Term not found"));
        if (in.examDate().isBefore(term.getStartDate()) || in.examDate().isAfter(term.getEndDate())) {
            throw new RuleException("The exam date has to fall within " + term.getName() + " ("
                    + term.getStartDate() + " to " + term.getEndDate() + ")");
        }
        var clazz = classRepo.findByIdAndSchool(in.classId(), schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));
        if (!sectionScopeService.current().allows(clazz.getLevel())) {
            throw new RuleException("That class isn't in your section");
        }
        subjectRepo.findByIdAndSchoolId(in.subjectId(), schoolId)
                .orElseThrow(() -> new NotFoundException("Subject not found"));
        String invigilatorName = null;
        if (in.invigilatorId() != null && !in.invigilatorId().isBlank()) {
            var teacher = teacherRepo.findByIdAndSchoolId(in.invigilatorId(), schoolId)
                    .orElseThrow(() -> new NotFoundException("Invigilator not found"));
            invigilatorName = teacher.getUser() != null ? teacher.getUser().getName() : "The invigilator";
        }

        var candidate = ExamSchedule.create("candidate", schoolId, in.termId(), in.classId(), in.subjectId(),
                in.paper(), in.examDate(), in.startTime(), in.endTime(), in.venue(), in.invigilatorId(), in.notes());
        for (var other : repo.findAllBySchoolIdAndExamDate(schoolId, in.examDate())) {
            if (other.getId().equals(selfId) || !candidate.overlaps(other)) {
                continue;
            }
            if (other.getClassId().equals(in.classId())) {
                throw new RuleException(clazz.getName() + " already sits " + describe(schoolId, other)
                        + " at that time");
            }
            if (candidate.getInvigilatorId() != null && candidate.getInvigilatorId().equals(other.getInvigilatorId())) {
                var otherClass = classRepo.findByIdAndSchool(other.getClassId(), schoolId)
                        .map(c -> c.getName()).orElse("another class");
                throw new RuleException(invigilatorName + " is already invigilating " + otherClass + " "
                        + describe(schoolId, other) + " at that time");
            }
        }
    }

    private String describe(String schoolId, ExamSchedule e) {
        var subject = subjectRepo.findByIdAndSchoolId(e.getSubjectId(), schoolId).map(s -> s.getName())
                .orElse("a paper");
        return subject + (e.getPaper() != null ? " (" + e.getPaper() + ")" : "") + ", "
                + TIME.format(e.getStartTime()) + "-" + TIME.format(e.getEndTime());
    }

    private ExamScheduleDTO toDTO(ResultSet rs, int i) throws SQLException {
        var invigilatorName = rs.getString("invigilator_name");
        return new ExamScheduleDTO(rs.getString("id"), rs.getString("term_id"), rs.getString("term_name"),
                rs.getString("class_id"), rs.getString("class_name"), rs.getString("level"),
                rs.getString("subject_id"), rs.getString("subject_name"), rs.getString("paper"),
                rs.getObject("exam_date", LocalDate.class), rs.getObject("start_time", LocalTime.class),
                rs.getObject("end_time", LocalTime.class), rs.getString("venue"), rs.getString("invigilator_id"),
                invigilatorName == null || invigilatorName.isBlank() ? null : invigilatorName,
                rs.getString("notes"));
    }
}
