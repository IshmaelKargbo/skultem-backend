package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Lesson;
import com.moriba.skultem.domain.repository.LessonRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateLessonStateUseCase {

    private final LessonRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "LESSON_STATE_UPDATED")
    public Lesson execute(String schoolId, String lessonId, Lesson.State state) {
        var lesson = repo.findById(lessonId).orElseThrow(() -> new NotFoundException("lesson not found"));

        lesson.setState(state);
        repo.save(lesson);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                "Lesson note marked " + state.name().toLowerCase().replace('_', ' '),
                lesson.getTitle(),
                null,
                lesson.getId());

        return lesson;
    }
}
