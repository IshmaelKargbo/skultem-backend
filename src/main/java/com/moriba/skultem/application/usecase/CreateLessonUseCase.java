package com.moriba.skultem.application.usecase;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Lesson;
import com.moriba.skultem.domain.repository.LessonRepository;
import com.moriba.skultem.domain.repository.WeekRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import com.moriba.skultem.domain.vo.LessonStage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateLessonUseCase {

    private final WeekRepository weekRepo;
    private final LessonRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LESSON_CREATED")
    public Lesson execute(
            String schoolId,
            String weekId,
            String title,
            String content,
            LocalDate date,
            String duration,
            List<String> objectives,
            String previousKnowledge,
            List<String> teachingAids,
            List<String> referenceMaterials,
            List<LessonStage> presentation,
            String evaluation,
            String assignment) {

        var week = weekRepo.findById(weekId).orElseThrow(() -> new NotFoundException("week not found"));

        if (repo.existsByWeekIdAndTitleAndSchoolId(weekId, title, schoolId)) {
            throw new RuleException("a lesson with this title already exists for this week");
        }

        var id = UUID.randomUUID().toString();
        var domain = Lesson.create(
                id,
                schoolId,
                week,
                title,
                content,
                date,
                duration,
                objectives,
                previousKnowledge,
                teachingAids,
                referenceMaterials,
                presentation,
                evaluation,
                assignment);

        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Lesson note created",
                domain.getTitle(),
                null,
                domain.getId());

        return domain;
    }
}
