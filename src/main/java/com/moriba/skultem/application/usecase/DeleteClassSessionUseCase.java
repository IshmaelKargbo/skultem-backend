package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.ClassPurgeRepository;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Removes one class session - e.g. an "SSS 1 Science B" that isn't running - without touching the rest
// of the class. Refuses while students are placed in it.
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteClassSessionUseCase {

    private final ClassSessionRepository sessionRepo;
    private final ClassPurgeRepository purgeRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "CLASS_SESSION_DELETED")
    public void execute(String schoolId, String sessionId) {
        var session = sessionRepo.findByIdAndSchoolId(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        if (purgeRepo.studentsInSession(schoolId, sessionId) > 0) {
            throw new RuleException(session.getName() + " has students in it, so it can't be removed. Move the "
                    + "students to another class first.");
        }

        purgeRepo.purgeSession(schoolId, sessionId);

        logActivityUseCase.log(schoolId, ActivityType.CLASS, "Class section removed", session.getName(), null,
                session.getClazz().getId());
    }
}
