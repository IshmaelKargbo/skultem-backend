package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.WorkingDay;
import com.moriba.skultem.domain.repository.TimingRepository;
import com.moriba.skultem.domain.repository.WorkingDayRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SetWorkingDayUseCase {

    private final TimingRepository timingRepo;
    private final WorkingDayRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "WORKING_DAY_SET")
    public List<WorkingDay> execute(String schoolId, List<WorkingDayRecord> days) {

        var timing = timingRepo.findBySchoolId(schoolId)
                .orElseThrow(() -> new NotFoundException("No school timing has been configured yet"));

        List<WorkingDay> result = new ArrayList<>();

        for (WorkingDayRecord record : days) {

            WorkingDay domain;

            if (repo.existsByDayAndSchoolId(record.day(), schoolId)) {

                domain = repo.findByDayAndSchoolId(record.day(), schoolId)
                        .orElseThrow(() -> new NotFoundException("Working day not found"));

                domain.setState(record.state());

            } else {
                domain = WorkingDay.create(
                        UUID.randomUUID().toString(),
                        schoolId,
                        timing,
                        record.day(),
                        record.state()
                );
            }

            repo.save(domain);
            result.add(domain);
        }

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Working days updated",
                "Configured " + days.size() + " working days",
                null,
                timing.getId()
        );

        return result;
    }

    public record WorkingDayRecord(
            WorkingDay.Day day,
            boolean state
    ) {
    }
}