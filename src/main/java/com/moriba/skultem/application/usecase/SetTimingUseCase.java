package com.moriba.skultem.application.usecase;

import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.model.WorkingDay;
import com.moriba.skultem.domain.repository.TimingRepository;
import com.moriba.skultem.domain.repository.WorkingDayRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SetTimingUseCase {
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
    public Timing execute(String schoolId, LocalTime startTime, LocalTime endTime, int periodDuration,
                             int breakDuration, int lunchDuration) {
        Timing domain;

        if (!repo.existsBySchoolId(schoolId)) {
            domain = Timing.create(
                    UUID.randomUUID().toString(),
                    schoolId,
                    startTime,
                    endTime,
                    periodDuration,
                    breakDuration,
                    lunchDuration
            );
        } else {
            domain = repo.findBySchoolId(schoolId)
                    .orElseThrow(() -> new RuntimeException("Timing not found"));

            domain.updateSchedule(startTime, endTime, periodDuration,
                    breakDuration, lunchDuration);
        }

        repo.save(domain);

        var days = workingDayRepo.findAllBySchoolId(schoolId);
        if (days.isEmpty()) {
            defaultWorkingDay(schoolId);
        }

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Timing updated",
                startTime + " - " + endTime,
                null,
                domain.getId()
        );

        return domain;
    }

    private void defaultWorkingDay(String school){
        setWorkingDayUseCase.execute(school, DEFAULT_WORKING_DAYS);
    }
}