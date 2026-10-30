package com.moriba.skultem.application.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moriba.skultem.application.dto.LibraryAttendanceOverviewDTO;
import com.moriba.skultem.application.dto.LibraryAttendanceTotalsDTO;
import com.moriba.skultem.application.dto.LibraryCheckInResultDTO;
import com.moriba.skultem.application.dto.LibrarySessionDTO;
import com.moriba.skultem.application.dto.LibrarySessionDetailDTO;
import com.moriba.skultem.application.dto.LibraryStudentHistoryDTO;
import com.moriba.skultem.application.dto.LibraryVisitDTO;
import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.mapper.LibraryAttendanceDtoMapper;
import com.moriba.skultem.application.usecase.AdjustLibraryRosterUseCase;
import com.moriba.skultem.application.usecase.CancelLibrarySessionUseCase;
import com.moriba.skultem.application.usecase.CheckInLibraryVisitorsUseCase;
import com.moriba.skultem.application.usecase.CheckOutLibraryVisitUseCase;
import com.moriba.skultem.application.usecase.CorrectLibraryAttendanceUseCase;
import com.moriba.skultem.application.usecase.FinalizeLibrarySessionUseCase;
import com.moriba.skultem.application.usecase.MarkLibrarySessionUseCase;
import com.moriba.skultem.application.usecase.MarkLibrarySessionUseCase.Mark;
import com.moriba.skultem.application.usecase.OpenLibrarySessionUseCase;
import com.moriba.skultem.application.usecase.ResolveLibraryVisitUseCase;
import com.moriba.skultem.domain.model.LibrarySession;
import com.moriba.skultem.domain.model.LibrarySessionRecord;
import com.moriba.skultem.domain.model.LibrarySessionRecord.Status;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.LibraryAttendanceCorrectionRepository;
import com.moriba.skultem.domain.repository.LibraryRosterChangeRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRecordRepository;
import com.moriba.skultem.domain.repository.LibrarySessionRepository;
import com.moriba.skultem.domain.repository.LibraryVisitRepository;
import com.moriba.skultem.domain.repository.LibraryVisitRepository.Filter;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.domain.shared.SchoolTimeZone;

import lombok.RequiredArgsConstructor;

/**
 * Library Attendance (docs/LIBRARY_ATTENDANCE.md): reads, plus a thin front for the use cases that change
 * things. Independent visits and scheduled sessions are kept apart everywhere - no figure here adds them up.
 */
@Service
@RequiredArgsConstructor
public class LibraryAttendanceService {

    /** The session list's status filter. */
    public enum SessionFilter {
        ALL, UPCOMING, IN_PROGRESS, COMPLETED, CANCELLED, INCOMPLETE
    }

    private final LibraryVisitRepository visitRepo;
    private final LibrarySessionRepository sessionRepo;
    private final LibrarySessionRecordRepository recordRepo;
    private final LibraryRosterChangeRepository changeRepo;
    private final LibraryAttendanceCorrectionRepository correctionRepo;
    private final StudentRepository studentRepo;
    private final LibrarySessionSchedule schedule;
    private final LibraryAttendanceSupport support;

    private final CheckInLibraryVisitorsUseCase checkInUseCase;
    private final CheckOutLibraryVisitUseCase checkOutUseCase;
    private final ResolveLibraryVisitUseCase resolveUseCase;
    private final OpenLibrarySessionUseCase openUseCase;
    private final MarkLibrarySessionUseCase markUseCase;
    private final AdjustLibraryRosterUseCase rosterUseCase;
    private final FinalizeLibrarySessionUseCase finalizeUseCase;
    private final CancelLibrarySessionUseCase cancelUseCase;
    private final CorrectLibraryAttendanceUseCase correctUseCase;

    // Overview & settings

    @Transactional(readOnly = true)
    public LibraryAttendanceOverviewDTO overview(String schoolId) {
        Instant now = support.now();
        LocalDate today = support.today();
        long visitsToday = visitRepo.search(schoolId, startOf(today), startOf(today.plusDays(1)), null, null,
                Filter.ALL, now, PageRequest.of(0, 1)).getTotalElements();

        var todayRows = sessionRows(schoolId, today, today);
        int sessionsToday = (int) todayRows.stream().filter(r -> !"CANCELLED".equals(r.status())).count();

        // Incomplete: anything from the start of the current term up to today.
        int incomplete = schedule.load(schoolId)
                .flatMap(c -> c.terms().stream()
                        .filter(t -> t.getStartDate() != null && t.getEndDate() != null)
                        .filter(t -> !today.isBefore(t.getStartDate()) && !today.isAfter(t.getEndDate()))
                        .map(Term::getStartDate)
                        .findFirst())
                .map(start -> (int) sessionRows(schoolId, start, today).stream()
                        .filter(LibrarySessionDTO::incomplete).count())
                .orElse(0);

        int libraryPeriods = schedule.load(schoolId).map(c -> c.entries().size()).orElse(0);

        return new LibraryAttendanceOverviewDTO(libraryPeriods, visitRepo.countOpen(schoolId),
                visitRepo.countUnresolved(schoolId, now), visitsToday, sessionsToday, incomplete);
    }

    // Workflow A - independent visits

    public LibraryCheckInResultDTO checkIn(String schoolId, List<String> studentIds, boolean eligibleOnly,
            String userId) {
        return checkInUseCase.execute(schoolId, studentIds, eligibleOnly, userId);
    }

    public LibraryVisitDTO checkOut(String schoolId, String visitId, String userId) {
        return checkOutUseCase.execute(schoolId, visitId, userId);
    }

    public LibraryVisitDTO resolve(String schoolId, String visitId, String reason, String userId) {
        return resolveUseCase.execute(schoolId, visitId, reason, userId);
    }

    /** Visits checked in on dates {@code from}..{@code to} (school days, both included; null = no limit). */
    @Transactional(readOnly = true)
    public Page<LibraryVisitDTO> listVisits(String schoolId, int page, int size, LocalDate from, LocalDate to,
            String studentId, String search, Filter filter) {
        Instant now = support.now();
        return visitRepo.search(schoolId, from == null ? null : startOf(from),
                to == null ? null : startOf(to.plusDays(1)), blankToNull(studentId), trim(search), filter, now,
                pageable(page, size))
                .map(v -> LibraryAttendanceDtoMapper.toDTO(v, now));
    }

    // Workflow B - scheduled sessions

    @Transactional(readOnly = true)
    public List<LibrarySessionDTO> listSessions(String schoolId, LocalDate from, LocalDate to, String classSessionId,
            SessionFilter filter) {
        requireRange(from, to);
        var rows = sessionRows(schoolId, from, to).stream();
        if (classSessionId != null && !classSessionId.isBlank()) {
            rows = rows.filter(r -> classSessionId.equals(r.classSessionId()));
        }
        SessionFilter f = filter == null ? SessionFilter.ALL : filter;
        return rows.filter(r -> switch (f) {
            case ALL -> true;
            case INCOMPLETE -> r.incomplete();
            default -> f.name().equals(r.status());
        }).toList();
    }

    @Transactional(readOnly = true)
    public LibrarySessionDetailDTO session(String schoolId, String sessionId) {
        var session = sessionRepo.findByIdAndSchool(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Library session not found"));
        return LibraryAttendanceDtoMapper.toDetail(session, recordRepo.findBySession(sessionId, schoolId),
                changeRepo.findBySession(sessionId, schoolId), correctionRepo.findBySession(sessionId, schoolId),
                support.localNow(), support.today());
    }

    public LibrarySessionDetailDTO open(String schoolId, String timetableEntryId, LocalDate date, String userId) {
        return session(schoolId, openUseCase.execute(schoolId, timetableEntryId, date, userId));
    }

    public LibrarySessionDetailDTO mark(String schoolId, String sessionId, List<Mark> marks, String userId) {
        markUseCase.execute(schoolId, sessionId, marks, userId);
        return session(schoolId, sessionId);
    }

    public LibrarySessionDetailDTO addToRoster(String schoolId, String sessionId, String studentId, String reason,
            String userId) {
        rosterUseCase.addStudent(schoolId, sessionId, studentId, reason, userId);
        return session(schoolId, sessionId);
    }

    public LibrarySessionDetailDTO removeFromRoster(String schoolId, String sessionId, String recordId,
            String reason, String userId) {
        rosterUseCase.removeStudent(schoolId, sessionId, recordId, reason, userId);
        return session(schoolId, sessionId);
    }

    public LibrarySessionDetailDTO finalizeSession(String schoolId, String sessionId, String userId) {
        finalizeUseCase.execute(schoolId, sessionId, userId);
        return session(schoolId, sessionId);
    }

    public LibrarySessionDetailDTO cancel(String schoolId, String sessionId, String reason, String userId) {
        return session(schoolId, cancelUseCase.cancelOpened(schoolId, sessionId, reason, userId));
    }

    public LibrarySessionDetailDTO cancelUpcoming(String schoolId, String timetableEntryId, LocalDate date,
            String reason, String userId) {
        return session(schoolId, cancelUseCase.cancelUpcoming(schoolId, timetableEntryId, date, reason, userId));
    }

    public LibrarySessionDetailDTO correct(String schoolId, String sessionId, String recordId, Status status,
            String excuseReason, String reason, String userId) {
        correctUseCase.execute(schoolId, sessionId, recordId, status, excuseReason, reason, userId);
        return session(schoolId, sessionId);
    }

    // Reports

    @Transactional(readOnly = true)
    public LibraryStudentHistoryDTO studentHistory(String schoolId, String studentId) {
        var student = studentRepo.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Student not found"));
        Instant now = support.now();
        var visits = visitRepo.search(schoolId, null, null, studentId, "", Filter.ALL, now, Pageable.unpaged())
                .map(v -> LibraryAttendanceDtoMapper.toDTO(v, now)).getContent();

        var records = recordRepo.findByStudent(studentId, schoolId).stream()
                .filter(LibrarySessionRecord::isCounted)
                .toList();
        Map<String, LibrarySession> sessions = sessionRepo
                .findByIds(schoolId, records.stream().map(LibrarySessionRecord::getSessionId).toList()).stream()
                .collect(Collectors.toMap(LibrarySession::getId, s -> s));
        var rows = records.stream()
                .filter(r -> sessions.containsKey(r.getSessionId()))
                .map(r -> {
                    var s = sessions.get(r.getSessionId());
                    return new LibraryStudentHistoryDTO.Session(s.getId(), s.getDate(), s.getClassName(),
                            s.getPeriodName(), r.getStatus().name(), s.getStatus().name(), r.isCorrected());
                })
                .sorted(Comparator.comparing(LibraryStudentHistoryDTO.Session::date).reversed())
                .toList();
        return new LibraryStudentHistoryDTO(student.getId(), student.getName(), visits, rows);
    }

    /** Status totals from finalized sessions only. Cancelled and incomplete sessions are counted separately. */
    @Transactional(readOnly = true)
    public LibraryAttendanceTotalsDTO totals(String schoolId, LocalDate from, LocalDate to, String classSessionId) {
        var rows = listSessions(schoolId, from, to, classSessionId, SessionFilter.ALL);
        Map<String, int[]> byClass = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        int[] all = new int[4];
        for (var r : rows) {
            if (!"COMPLETED".equals(r.status())) {
                continue;
            }
            var c = byClass.computeIfAbsent(r.classSessionId(), k -> new int[4]);
            names.putIfAbsent(r.classSessionId(), r.className());
            for (int[] t : List.of(c, all)) {
                t[0]++;
                t[1] += r.present();
                t[2] += r.absent();
                t[3] += r.excused();
            }
        }
        List<LibraryAttendanceTotalsDTO.Row> classes = new ArrayList<>();
        byClass.forEach((id, t) -> classes.add(new LibraryAttendanceTotalsDTO.Row(id, names.get(id), t[0], t[1], t[2],
                t[3])));
        classes.sort(Comparator.comparing(LibraryAttendanceTotalsDTO.Row::className,
                Comparator.nullsLast(Comparator.naturalOrder())));
        int incomplete = (int) rows.stream().filter(LibrarySessionDTO::incomplete).count();
        int cancelled = (int) rows.stream().filter(r -> "CANCELLED".equals(r.status())).count();
        return new LibraryAttendanceTotalsDTO(from, to, classes,
                new LibraryAttendanceTotalsDTO.Row(null, "All classes", all[0], all[1], all[2], all[3]), incomplete,
                cancelled);
    }

    // Helpers

    /** Stored sessions in the range, plus the timetable's sessions nobody has opened or cancelled yet. */
    private List<LibrarySessionDTO> sessionRows(String schoolId, LocalDate from, LocalDate to) {
        LocalDateTime now = support.localNow();
        LocalDate today = support.today();

        var stored = sessionRepo.findBetween(schoolId, from, to);
        Map<String, List<LibrarySessionRecord>> recordsBySession = recordRepo
                .findBySessions(stored.stream().map(LibrarySession::getId).toList(), schoolId).stream()
                .collect(Collectors.groupingBy(LibrarySessionRecord::getSessionId));
        Set<String> taken = stored.stream()
                .filter(s -> s.getTimetableEntryId() != null)
                .map(s -> s.getTimetableEntryId() + "|" + s.getDate())
                .collect(Collectors.toSet());

        List<LibrarySessionDTO> rows = new ArrayList<>();
        for (var s : stored) {
            rows.add(LibraryAttendanceDtoMapper.toDTO(s, recordsBySession.getOrDefault(s.getId(), List.of()), now,
                    today));
        }
        schedule.load(schoolId).ifPresent(calendar -> schedule.slotsBetween(calendar, from, to).stream()
                .filter(slot -> !taken.contains(slot.timetableEntryId() + "|" + slot.date()))
                .forEach(slot -> rows.add(LibraryAttendanceDtoMapper.upcoming(slot, now, today))));

        rows.sort(Comparator.comparing(LibrarySessionDTO::date)
                .thenComparing(LibrarySessionDTO::startTime)
                .thenComparing(LibrarySessionDTO::className, Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    private static void requireRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BadRequestException("Choose a date range");
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("The end date must be on or after the start date");
        }
        if (ChronoUnit.DAYS.between(from, to) >= LibrarySessionSchedule.MAX_DAYS) {
            throw new BadRequestException("Choose a range of at most " + LibrarySessionSchedule.MAX_DAYS + " days");
        }
    }

    private static Instant startOf(LocalDate date) {
        return date.atStartOfDay(SchoolTimeZone.ZONE).toInstant();
    }

    private static Pageable pageable(int page, int size) {
        return size > 0 ? PageRequest.of(Math.max(page, 1) - 1, size) : Pageable.unpaged();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
