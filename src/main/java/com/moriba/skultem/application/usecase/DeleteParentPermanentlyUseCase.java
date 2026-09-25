package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.ParentPurgeRepository;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// For a guardian added by mistake. A student can't exist without a primary guardian, so this refuses
// while any student still has this parent - delete or move those students first. The confirmation is
// the guardian's full name typed out.
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteParentPermanentlyUseCase {

    public record Result(String parentName, int otherStudentLinksRemoved, String account) {
    }

    private final ParentRepository parentRepo;
    private final ParentPurgeRepository purgeRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "PARENT_DELETED_PERMANENTLY")
    public Result execute(String schoolId, String parentId, String confirmation) {
        var parent = parentRepo.findByIdAndSchoolId(parentId, schoolId)
                .orElseThrow(() -> new NotFoundException("Parent not found"));

        String name = parent.getUser().getGivenNames() + " " + parent.getUser().getFamilyName();
        if (confirmation == null || !normalize(confirmation).equals(normalize(name))) {
            throw new RuleException("Type the guardian's full name (" + name + ") to confirm the deletion.");
        }

        long students = purgeRepo.primaryStudentCount(schoolId, parentId);
        if (students > 0) {
            throw new RuleException(name + " is still the guardian of " + students + " student"
                    + (students == 1 ? "" : "s") + ". Delete those students (or link them to another guardian) first.");
        }

        var purged = purgeRepo.purge(schoolId, parentId);

        logActivityUseCase.log(schoolId, ActivityType.PARENT, "Parent permanently deleted", name,
                purged.otherStudentLinksRemoved() + " other student link(s) removed", null);

        return new Result(name, purged.otherStudentLinksRemoved(), purged.account().name());
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
