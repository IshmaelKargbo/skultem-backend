package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.ClassPurgeRepository;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// For a class set up by mistake (wrong name, wrong level): removes the class with its sessions,
// sections, streams, subjects and setup. Refuses once anything real hangs off it - students placed in
// it, report cards, fees already billed - so history can't be lost; move the students first.
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteClassUseCase {

    private final ClassRepository classRepo;
    private final ClassPurgeRepository purgeRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "CLASS_DELETED")
    public void execute(String schoolId, String classId) {
        var clazz = classRepo.findByIdAndSchool(classId, schoolId)
                .orElseThrow(() -> new NotFoundException("Class not found"));

        int records = purgeRepo.recordsInClass(schoolId, classId);
        if (records > 0) {
            throw new RuleException(clazz.getName() + " has students or report cards recorded against it, so it "
                    + "can't be deleted. Move its students to another class first (a class that's simply "
                    + "finished is left in place - it keeps the history).");
        }
        if (purgeRepo.hasBilledFees(schoolId, classId)) {
            throw new RuleException(clazz.getName() + " has fees that students have already been billed for or "
                    + "have paid, so it can't be deleted.");
        }

        purgeRepo.purgeClass(schoolId, classId);

        logActivityUseCase.log(schoolId, ActivityType.CLASS, "Class deleted",
                clazz.getName() + " (" + clazz.getLevel().name() + ")", null, classId);
    }
}
