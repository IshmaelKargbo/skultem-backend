package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.error.NotFoundException;
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
    public Timetable execut(String schoolId, String periodId, String subject, Day day, String roomId, String color) {
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

        var domain = repo.findByPeriodIdAndDayIdAndSchoolId(periodId, workingDay.getId(), schoolId).orElse(null);

        if (domain == null) {
            var id = UUID.randomUUID().toString();
            domain = Timetable.create(id, schoolId, period, teacherSubject, workingDay, room, color);
        } else {
            domain.update(teacherSubject, room, color);
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
