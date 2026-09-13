package com.moriba.skultem.application.usecase;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.model.WorkingDay;
import com.moriba.skultem.domain.repository.TimingRepository;
import com.moriba.skultem.domain.repository.WorkingDayRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import lombok.RequiredArgsConstructor;

// Create or update a Timing template. id == null creates a new one - the very first template a
// school saves becomes its default automatically (there must always be exactly one default, and
// nothing else has assigned it yet); later templates are created as plain (non-default) ones,
// promoted via SetDefaultTimingUseCase if the admin wants to swap the default.
@Service
@RequiredArgsConstructor
public class SaveTimingUseCase {
    private final TimingRepository repo;
    private final WorkingDayRepository workingDayRepo;
    private final SetWorkingDayUseCase setWorkingDayUseCase;
    private final LogActivityUseCase logActivityUseCase;
    private static final List<SetWorkingDayUseCase.WorkingDayRecord> DEFAULT_WORKING_DAYS = List.of(
            new SetWorkingDayUseCase.WorkingDayRecord(WorkingDay.Day.MONDAY, true),
            new SetWorkingDayUseCase.WorkingDayRecord(WorkingDay.Day.TUESDAY, true),
            new SetWorkingDayUseCase.WorkingDayRecord(WorkingDay.Day.WEDNESDAY, true),
            new SetWorkingDayUseCase.WorkingDayRecord(WorkingDay.Day.THURSDAY, true),
            new SetWorkingDayUseCase.WorkingDayRecord(WorkingDay.Day.FRIDAY, true),
            new SetWorkingDayUseCase.WorkingDayRecord(WorkingDay.Day.SATURDAY, false),
            new SetWorkingDayUseCase.WorkingDayRecord(WorkingDay.Day.SUNDAY, false)
    );

    @AuditLogAnnotation(action = "TIMING_SET")
    public Timing execute(String schoolId, String id, String name, LocalTime startTime, LocalTime endTime,
                           int periodDuration, int breakDuration, int lunchDuration) {
        Timing domain;

        if (id == null) {
            boolean isFirst = !repo.existsDefaultBySchoolId(schoolId);
            domain = Timing.create(
                    UUID.randomUUID().toString(),
                    schoolId,
                    name,
                    isFirst,
                    startTime,
                    endTime,
                    periodDuration,
                    breakDuration,
                    lunchDuration
            );
        } else {
            domain = repo.findByIdAndSchoolId(id, schoolId)
                    .orElseThrow(() -> new NotFoundException("Timing template not found"));

            domain.updateSchedule(name, startTime, endTime, periodDuration, breakDuration, lunchDuration);
        }

        repo.save(domain);

        if (workingDayRepo.findAllByTimingId(domain.getId()).isEmpty()) {
            setWorkingDayUseCase.execute(schoolId, domain.getId(), DEFAULT_WORKING_DAYS);
        }

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                id == null ? "Timing template created" : "Timing template updated",
                name + " (" + startTime + " - " + endTime + ")",
                null,
                domain.getId()
        );

        return domain;
    }
}
