package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.error.BadRequestException;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Period;
import com.moriba.skultem.domain.repository.PeriodRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

// Lets an admin nudge one period's start/end time on a single class's timetable. A Period always
// belongs to exactly one ClassSession (see Period.session), so this can never bleed into another
// class - unlike Timing templates, which are shared by every class in a Level.
@Service
@RequiredArgsConstructor
public class UpdatePeriodUseCase {

    private final PeriodRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "PERIOD_UPDATED")
    public Period execute(String schoolId, String id, LocalTime startTime, LocalTime endTime) {

        Period period = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("No period found"));

        if (!period.getSchoolId().equals(schoolId)) {
            throw new NotFoundException("No period found");
        }

        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("Start time must be before end time");
        }

        period.updateSchedule(startTime, endTime);
        repo.save(period);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Period time updated",
                period.getName() + " (" + startTime + " - " + endTime + ")",
                null,
                period.getId()
        );

        return period;
    }
}
