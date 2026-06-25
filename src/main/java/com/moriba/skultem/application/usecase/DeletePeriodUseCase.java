package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Period;
import com.moriba.skultem.domain.model.Timetable;
import com.moriba.skultem.domain.repository.PeriodRepository;
import com.moriba.skultem.domain.repository.TimetableRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class DeletePeriodUseCase {

    private final PeriodRepository periodRepo;
    private final TimetableRepository timetableRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "PERIOD_DELETED")
    public Period execute(String id) {

        Period period = periodRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("No period found"));

        // Delete all timetable entries linked to this period
        var timetables = timetableRepo.findAllByPeriodId(id);

        for (Timetable timetable : timetables) {
            timetableRepo.delete(timetable);
        }

        periodRepo.delete(period);

        logActivityUseCase.log(
                period.getSchoolId(),
                ActivityType.SCHOOL,
                "Period deleted",
                "Deleted period '" + period.getName() + "'",
                null,
                period.getId()
        );
        
        return period;
    }
}