package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.LibraryCheckInResultDTO;
import com.moriba.skultem.application.dto.LibraryCheckInResultDTO.Refusal;
import com.moriba.skultem.application.dto.LibraryVisitDTO;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.LibraryAttendanceDtoMapper;
import com.moriba.skultem.application.services.LibraryAttendanceSupport;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.model.LibrarySession;
import com.moriba.skultem.domain.model.LibraryVisit;
import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRecordRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRepository;
import com.moriba.skultem.domain.repository.LibraryVisitRepository;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.shared.SchoolTimeZone;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Checks one or more students in to the library (Workflow A: A-1 to A-3, AR-1, AR-5, AR-7).
 * <p>
 * All or nothing: if any selected student can't be checked in (already in, not currently enrolled, not in this
 * school), nobody is, and the result says who and why. The librarian can then choose "check in the eligible
 * students only", which saves the rest - again all together or not at all. A student can only have one open
 * visit; the database refuses a second one even when two check-ins arrive at the same moment.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CheckInLibraryVisitorsUseCase {

    public static final int MAX_STUDENTS = 100;

    private final StudentRepository studentRepo;
    private final AcademicYearRepository academicYearRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final LibraryVisitRepository visitRepo;
    private final LibrarySessionRepository sessionRepo;
    private final LibrarySessionRecordRepository recordRepo;
    private final ResolveTimingForLevelUseCase resolveTimingUseCase;
    private final LibraryAttendanceSupport support;
    private final LogActivityUseCase logActivityUseCase;

    private record Eligible(Student student, Enrollment enrollment) {
    }

    @AuditLogAnnotation(action = "LIBRARY_VISIT_CHECKED_IN")
    public LibraryCheckInResultDTO execute(String schoolId, List<String> studentIds, boolean eligibleOnly,
            String userId) {
        var ids = new LinkedHashSet<>(studentIds == null ? List.<String>of() : studentIds);
        ids.removeIf(id -> id == null || id.isBlank());
        if (ids.isEmpty()) {
            throw new BadRequestException("Select at least one student");
        }
        if (ids.size() > MAX_STUDENTS) {
            throw new BadRequestException("Check in at most " + MAX_STUDENTS + " students at a time");
        }

        var year = academicYearRepo.findActiveBySchool(schoolId)
                .orElseThrow(() -> new NotFoundException("No active academic year found"));
        Map<String, LibraryVisit> open = visitRepo.findOpenByStudents(schoolId, ids).stream()
                .collect(Collectors.toMap(LibraryVisit::getStudentId, Function.identity(), (a, b) -> a));
        Instant now = support.now();

        List<Eligible> eligible = new ArrayList<>();
        List<Refusal> refused = new ArrayList<>();
        for (String id : ids) {
            var student = studentRepo.findByIdAndSchoolId(id, schoolId).orElse(null);
            if (student == null) {
                refused.add(new Refusal(id, null, "Student not found"));
                continue;
            }
            var visit = open.get(id);
            if (visit != null) {
                refused.add(new Refusal(id, student.getName(), visit.isUnresolved(now)
                        ? "Has a visit from an earlier school day that was never checked out. Follow it up first."
                        : "Is already checked in"));
                continue;
            }
            var enrollment = enrollmentRepo.findByStudentAndAcademicYearAndSchoolId(id, year.getId(), schoolId)
                    .orElse(null);
            if (student.getStatus() != Student.Status.ACTIVE || enrollment == null
                    || enrollment.getStatus() != Enrollment.Status.ACTIVE) {
                refused.add(new Refusal(id, student.getName(), "Is not currently enrolled"));
                continue;
            }
            eligible.add(new Eligible(student, enrollment));
        }

        if (!refused.isEmpty() && !eligibleOnly) {
            return new LibraryCheckInResultDTO(List.of(), refused, List.of());
        }

        String userName = support.userName(userId);
        List<LibraryVisitDTO> checkedIn = new ArrayList<>();
        for (Eligible e : eligible) {
            var visit = LibraryVisit.checkIn(schoolId, e.student().getId(), e.student().getName(),
                    LibraryAttendanceSupport.className(e.enrollment()), userId, userName, now,
                    schoolDayEnd(schoolId, e.enrollment(), now));
            LibraryAttendanceSupport.saveUnique(() -> visitRepo.save(visit),
                    visit.getStudentName() + " is already checked in to the library");
            checkedIn.add(LibraryAttendanceDtoMapper.toDTO(visit, now));
        }

        if (!checkedIn.isEmpty()) {
            logActivityUseCase.log(schoolId, ActivityType.LIBRARY, "Library check-in",
                    checkedIn.size() == 1 ? checkedIn.get(0).studentName() : checkedIn.size() + " students",
                    null, checkedIn.get(0).id());
        }

        return new LibraryCheckInResultDTO(checkedIn, refused, duringClassSession(schoolId, eligible, now));
    }

    /**
     * Students who are on the register of a library session that is in progress right now (AR-8). Only a warning:
     * their visit is still recorded, as an independent visit, and the register is not touched.
     */
    private List<Refusal> duringClassSession(String schoolId, List<Eligible> students, Instant now) {
        if (students.isEmpty()) {
            return List.of();
        }
        var local = now.atZone(SchoolTimeZone.ZONE);
        LocalDate today = local.toLocalDate();
        LocalTime time = local.toLocalTime();
        Map<String, LibrarySession> running = sessionRepo.findBetween(schoolId, today, today).stream()
                .filter(LibrarySession::isInProgress)
                .filter(s -> !time.isBefore(s.getStartTime()) && !time.isAfter(s.getEndTime()))
                .collect(Collectors.toMap(LibrarySession::getId, Function.identity()));
        if (running.isEmpty()) {
            return List.of();
        }
        Map<String, LibrarySession> sessionByStudent = new HashMap<>();
        for (var record : recordRepo.findBySessions(running.keySet(), schoolId)) {
            if (record.isCounted()) {
                sessionByStudent.putIfAbsent(record.getStudentId(), running.get(record.getSessionId()));
            }
        }
        List<Refusal> warnings = new ArrayList<>();
        for (Eligible e : students) {
            var session = sessionByStudent.get(e.student().getId());
            if (session != null) {
                warnings.add(new Refusal(e.student().getId(), e.student().getName(),
                        "Is on the register for " + session.getClassName() + "'s library period now. "
                                + "Mark them on that register too - this visit doesn't count there."));
            }
        }
        return warnings;
    }

    /**
     * When this student's school day ends (D-4): the end time of the timing template for their class's level. A
     * student who comes in after that (an after-school visit) has until midnight. No timing set up: midnight too.
     */
    private Instant schoolDayEnd(String schoolId, Enrollment enrollment, Instant now) {
        LocalDate today = now.atZone(SchoolTimeZone.ZONE).toLocalDate();
        Instant midnight = today.plusDays(1).atStartOfDay(SchoolTimeZone.ZONE).toInstant();
        try {
            var level = enrollment.getClazz() == null ? null : enrollment.getClazz().getLevel();
            LocalTime end = resolveTimingUseCase.execute(schoolId, level).getEndTime();
            if (end == null) {
                return midnight;
            }
            Instant schoolEnd = ZonedDateTime.of(today, end, SchoolTimeZone.ZONE).toInstant();
            return schoolEnd.isAfter(now) ? schoolEnd : midnight;
        } catch (NotFoundException e) {
            return midnight;
        }
    }
}
