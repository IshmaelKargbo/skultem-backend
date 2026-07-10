package com.moriba.skultem.application.usecase;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Week;
import com.moriba.skultem.domain.repository.SchemeOfWorkRepository;
import com.moriba.skultem.domain.repository.WeekRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateWeekUseCase {

    private final SchemeOfWorkRepository schemeRepo;
    private final WeekRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "WEEK_CREATED")
    public Week execute(String schoolId, String schemeId, int week, String topic, String subtopic, List<String> objectives) {
        
        var scheme = schemeRepo.findById(schemeId).orElseThrow(() -> new NotFoundException("scheme not found"));

        if (repo.existsByWeekAndSchemeAndSchoolId(week, schemeId, schoolId))
           throw new RuleException("week already exist");

        var id = UUID.randomUUID().toString();
        var domain = Week.create(id, schoolId, week, topic, subtopic, objectives, scheme);
        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Week created",
                "week (" + domain.getWeek() + ")",
                null,
                domain.getId());

        return domain;
    }
}
