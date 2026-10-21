package com.moriba.skultem.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AttendanceMarkingComplianceDTO;
import com.moriba.skultem.application.dto.AttendanceMarkingComplianceDTO.ClassMarking;
import com.moriba.skultem.application.services.SectionScopeService;
import com.moriba.skultem.domain.repository.AttendanceRepository;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.domain.service.AttendanceRateCalculator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// See AttendanceMarkingComplianceDTO. A class counts as marked on a day when any of its students has
// a non-holiday record for it, and only owes registers from when it got its first student - a class
// set up or filled mid-term isn't charged for the weeks before. Every class session in the caller's
// section scope is listed, including ones never marked at all, in the class dropdown's order.
@Service
@Transactional
@RequiredArgsConstructor
public class GetAttendanceMarkingComplianceUseCase {

    private final ResolveTermSchoolDaysUseCase resolveTermSchoolDaysUseCase;
    private final AttendanceRepository attendanceRepo;
    private final ClassSessionRepository classSessionRepo;
    private final ClassMasterRepository classMasterRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final SectionScopeService sectionScopeService;

    public AttendanceMarkingComplianceDTO execute(String schoolId, String academicYearId, String termId) {
        var window = resolveTermSchoolDaysUseCase.execute(schoolId, academicYearId, termId);
        var term = window.term();
        var schoolDays = window.schoolDays();
        var levels = sectionScopeService.levels();
        String yearId = window.academicYear().getId();

        Map<String, TreeSet<LocalDate>> markedBySession = new HashMap<>();
        if (!schoolDays.isEmpty()) {
            for (Object[] row : attendanceRepo.markedDaysByClassSession(schoolId, yearId, levels,
                    term.getStartDate(), window.asOf())) {
                markedBySession.computeIfAbsent(key(row[0], row[1], row[2]), k -> new TreeSet<>()).add((LocalDate) row[3]);
            }
        }

        Map<String, Instant> firstEnrolled = new HashMap<>();
        for (Object[] row : enrollmentRepo.firstEnrollmentByClassSession(schoolId, yearId, levels)) {
            firstEnrolled.put(key(row[0], row[1], row[2]), (Instant) row[3]);
        }

        var sessions = classSessionRepo.findBySchoolIdAndAcademicYearId(schoolId, yearId, levels, Pageable.unpaged())
                .getContent();

        List<ClassMarking> classes = sessions.stream().map(session -> {
            String streamId = session.getStream() != null ? session.getStream().getId() : null;
            String sectionId = session.getSection() != null ? session.getSection().getId() : null;
            String sessionKey = key(session.getClazz().getId(), sectionId, streamId);
            var marked = markedBySession.getOrDefault(sessionKey, new TreeSet<>());

            // Owed from the later of the term start and its first student; no students = nothing owed.
            Instant first = firstEnrolled.get(sessionKey);
            LocalDate from = first == null ? null : laterOf(term.getStartDate(),
                    first.atZone(ZoneId.systemDefault()).toLocalDate());
            var owed = first == null ? List.<LocalDate>of()
                    : schoolDays.stream().filter(d -> !d.isBefore(from)).toList();

            // Only owed school days count - a register saved on some other day doesn't make up for a missed one.
            var missing = owed.stream().filter(d -> !marked.contains(d)).toList();
            int markedDays = owed.size() - missing.size();

            var masters = classMasterRepo.findAllActiveBySessionIdAndSchoolId(session.getId(), schoolId).stream()
                    .map(cm -> cm.getTeacher().getUser().getName())
                    .toList();

            return new ClassMarking(
                    session.getId(),
                    session.getClazz().getName(),
                    session.getSection() != null ? session.getSection().getName() : null,
                    session.getStream() != null ? session.getStream().getName() : null,
                    masters,
                    from,
                    owed.size(),
                    markedDays,
                    owed.isEmpty() ? null : AttendanceRateCalculator.rate(markedDays, owed.size()),
                    marked.isEmpty() ? null : marked.last(),
                    missing);
        }).toList();

        return new AttendanceMarkingComplianceDTO(term.getId(), term.getName(), term.getStartDate(), window.asOf(),
                schoolDays.size(), classes);
    }

    private static LocalDate laterOf(LocalDate a, LocalDate b) {
        return a == null || b.isAfter(a) ? b : a;
    }

    private static String key(Object classId, Object sectionId, Object streamId) {
        return classId + "|" + sectionId + "|" + streamId;
    }
}
