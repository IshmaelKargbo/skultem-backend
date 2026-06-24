package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.*;
import com.moriba.skultem.domain.repository.*;
import com.moriba.skultem.domain.vo.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateTimetableUseCase {

    private final TimingRepository timingRepo;
    private final ClassSessionRepository sessionRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;
    private final PeriodRepository periodRepo;
    private final TimetableRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TIMETABLE_CREATED")
    private Period create(String schoolId, String periodId, String subject, String day, Room room, String color) {
        var period = periodRepo.findById(periodId)
                .orElseThrow(() -> new NotFoundException("No period found"));
        var teacherSubject = teacherSubjectRepo.findBySubjectIdAndSessionIdAndSchoolId(subject, period.getSession().getId(), schoolId);

//        var timing = timingRepo.findBySchoolId(schoolId)
//                .orElseThrow(() -> new NotFoundException("No timing found"));
//
//        var session = sessionRepo.findByIdAndSchoolId(sessionId, schoolId)
//                .orElseThrow(() -> new NotFoundException("No class found"));
//
//        String id = UUID.randomUUID().toString();
//
//        var duration = getDuration(session.getId(), timing, type);
//
//        var domain = Timetable.create(
//                id,
//                schoolId,
//                session,
//                type,
//                duration.name(),
//                duration.startTime(),
//                duration.endTime()
//        );
//
//        repo.save(domain);
//
//        logActivityUseCase.log(
//                schoolId,
//                ActivityType.SCHOOL,
//                type.name() + " created",
//                domain.getName(),
//                null,
//                domain.getId()
//        );
//
//        return domain;
        return null;
    }

}
