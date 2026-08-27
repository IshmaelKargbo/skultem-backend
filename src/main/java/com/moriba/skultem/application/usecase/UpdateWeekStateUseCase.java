package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Week;
import com.moriba.skultem.domain.repository.WeekRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateWeekStateUseCase {

    private final WeekRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "WEEK_STATE_UPDATED")
    public Week execute(String schoolId, String weekId, Week.State state) {
        var week = repo.findById(weekId).orElseThrow(() -> new NotFoundException("week not found"));

        week.setState(state);
        repo.save(week);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Week marked " + state.name().toLowerCase().replace('_', ' '),
                "week (" + week.getWeek() + ")",
                null,
                week.getId());

        return week;
    }
}
