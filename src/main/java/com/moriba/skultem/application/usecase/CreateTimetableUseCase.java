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

    @AuditLogAnnotation(action = "TIMETABLE_CREATED")
    // entryId set: edit that entry. Not set: add a subject to the slot - the only one, or another option when
    // students split between subjects in the same period (e.g. PHE or RME).
    public Timetable execut(String schoolId, String periodId, String subject, Day day, String roomId, String color,
            String entryId) {
        var period = periodRepo.findById(periodId)
                .orElseThrow(() -> new NotFoundException("no period found"));

        // The period's own class session decides which Timing template (and so which set of
        // working days) is in play - same rule CreatePeriodUseCase used to create the period.
        var clazz = period.getSession() != null ? period.getSession().getClazz() : null;
        var level = clazz != null ? clazz.getLevel() : null;
        var timing = resolveTimingForLevelUseCase.execute(schoolId, level);

        var workingDay = dayRepo.findByDayAndTimingId(day, timing.getId())
                .orElseThrow(() -> new NotFoundException("no working day found"));

        var teacherSubject = teacherSubjectRepo
                .findById(subject)
                .orElseThrow(() -> new NotFoundException("no subject found"));
                
        var room = roomRepo.findByIdAndSchoolId(roomId, schoolId)
                .orElseThrow(() -> new NotFoundException("no room found"));

        var slot = repo.findAllByPeriodIdAndDayIdAndSchoolId(periodId, workingDay.getId(), schoolId);
        var others = slot.stream().filter(t -> !t.getId().equals(entryId)).toList();

        // Options in one slot run side by side, so the same subject can't be in it twice. Sharing a room is
        // allowed (a school may have only one) - the form warns about it instead.
        String subjectId = teacherSubject.getSubject().getId();
        if (others.stream().anyMatch(t -> t.getTeacherSubject().getSubject().getId().equals(subjectId))) {
            throw new RuleException(teacherSubject.getSubject().getName() + " is already in this period");
        }

        Timetable domain;
        if (entryId != null && !entryId.isBlank()) {
            domain = slot.stream().filter(t -> t.getId().equals(entryId)).findFirst()
                    .orElseThrow(() -> new NotFoundException("timetable entry not found"));
            domain.update(teacherSubject, room, color);
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
                session.getName() + " set " + teacherSubject.getSubject().getName() + " for " + day,
                null,
                domain.getId());

        return domain;
    }

}
