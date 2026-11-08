package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.*;
import com.moriba.skultem.domain.model.WorkingDay.Day;
import com.moriba.skultem.domain.repository.*;
import com.moriba.skultem.domain.vo.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateTimetableUseCase {

    private final TeacherSubjectRepository teacherSubjectRepo;
    private final RoomRepository roomRepo;
    private final PeriodRepository periodRepo;
    private final WorkingDayRepository dayRepo;
    private final TimetableRepository repo;
    private final ResolveTimingForLevelUseCase resolveTimingForLevelUseCase;
    private final LogActivityUseCase logActivityUseCase;

    /**
     * A double period: the same lesson (or Library period) in this period and the one straight after it, the same
     * day - e.g. a science practical. Both are checked before either is saved; the next period has to be a lesson
     * period (not a break, lunch or assembly) and free for it.
     */
    @org.springframework.transaction.annotation.Transactional
    @AuditLogAnnotation(action = "TIMETABLE_DOUBLE_PERIOD_CREATED")
    // entryId set: an existing lesson is saved and extended into the next period.
    public java.util.List<Timetable> executeDouble(String schoolId, String periodId, String subject, Day day,
            String roomId, String color, String entryId, Timetable.Kind kind) {
        var period = periodRepo.findById(periodId)
                .orElseThrow(() -> new NotFoundException("no period found"));
        var ordered = periodRepo.findAllBySessionId(period.getSession().getId()).stream()
                .sorted(java.util.Comparator.comparing(Period::getStartTime))
                .toList();
        int at = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(periodId)) {
                at = i;
            }
        }
        if (at < 0 || at == ordered.size() - 1) {
            throw new RuleException("There's no period after " + period.getName() + " to make it a double period");
        }
        var next = ordered.get(at + 1);
        if (next.isBreak() || next.isLunch() || next.isAssembly()) {
            throw new RuleException(next.getName() + " comes straight after, so this can't be a double period");
        }
        var first = execut(schoolId, periodId, subject, day, roomId, color, entryId, kind);
        var second = execut(schoolId, next.getId(), subject, day, roomId, color, null, kind);
        return java.util.List.of(first, second);
    }

    @AuditLogAnnotation(action = "TIMETABLE_CREATED")
    // entryId set: edit that entry. Not set: add a subject to the slot - the only one, or another option when
    // students split between subjects in the same period (e.g. PHE or RME).
    // kind LIBRARY: a Library period instead of a subject - no subject or teacher (the librarian runs it), and it
    // takes the whole slot for the class, so it can't sit beside options.
    public Timetable execut(String schoolId, String periodId, String subject, Day day, String roomId, String color,
            String entryId, Timetable.Kind kind) {
        boolean library = kind == Timetable.Kind.LIBRARY;
        var period = periodRepo.findById(periodId)
                .orElseThrow(() -> new NotFoundException("no period found"));

        // The period's own class session decides which Timing template (and so which set of
        // working days) is in play - same rule CreatePeriodUseCase used to create the period.
        var clazz = period.getSession() != null ? period.getSession().getClazz() : null;
        var level = clazz != null ? clazz.getLevel() : null;
        var timing = resolveTimingForLevelUseCase.execute(schoolId, level);

        var workingDay = dayRepo.findByDayAndTimingId(day, timing.getId())
                .orElseThrow(() -> new NotFoundException("no working day found"));

        if (period.isBreak() || period.isLunch() || period.isAssembly()) {
            throw new RuleException("Breaks, lunch and assembly can't hold a lesson or a Library period");
        }

        if (!library && (subject == null || subject.isBlank())) {
            throw new RuleException("Subject is required");
        }
        var teacherSubject = library ? null
                : teacherSubjectRepo.findById(subject).orElseThrow(() -> new NotFoundException("no subject found"));

        var room = roomRepo.findByIdAndSchoolId(roomId, schoolId)
                .orElseThrow(() -> new NotFoundException("no room found"));

        var slot = repo.findAllByPeriodIdAndDayIdAndSchoolId(periodId, workingDay.getId(), schoolId);
        var others = slot.stream().filter(t -> !t.getId().equals(entryId)).toList();

        if (library) {
            if (!others.isEmpty()) {
                throw new RuleException("This period already has "
                        + String.join(", ", others.stream().map(Timetable::label).toList())
                        + ". A Library period takes the whole period for the class - remove "
                        + (others.size() == 1 ? "it" : "them") + " first.");
            }
        } else {
            if (others.stream().anyMatch(Timetable::isLibrary)) {
                throw new RuleException("This is a Library period for the class. Remove it before adding a subject.");
            }
            // Options in one slot run side by side, so the same subject can't be in it twice. Sharing a room is
            // allowed (a school may have only one) - the form warns about it instead.
            String subjectId = teacherSubject.getSubject().getId();
            if (others.stream().anyMatch(t -> t.getTeacherSubject().getSubject().getId().equals(subjectId))) {
                throw new RuleException(teacherSubject.getSubject().getName() + " is already in this period");
            }
        }

        Timetable domain;
        if (entryId != null && !entryId.isBlank()) {
            domain = slot.stream().filter(t -> t.getId().equals(entryId)).findFirst()
                    .orElseThrow(() -> new NotFoundException("timetable entry not found"));
            if (library) {
                domain.updateAsLibrary(room, color);
            } else {
                domain.update(teacherSubject, room, color);
            }
        } else if (library) {
            domain = Timetable.createLibrary(UUID.randomUUID().toString(), schoolId, period, workingDay, room, color);
        } else {
            domain = Timetable.create(UUID.randomUUID().toString(), schoolId, period, teacherSubject, workingDay,
                    room, color);
        }
        repo.save(domain);

        var session = period.getSession();

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Period  created",
                session.getName() + " set " + domain.label() + " for " + day,
                null,
                domain.getId());

        return domain;
    }

}
