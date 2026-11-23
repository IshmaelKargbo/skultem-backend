package com.moriba.skultem.application.usecase;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.TeacherPurgeRepository;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * For a teacher or staff member added by mistake. Refused while anything they did or were given is on record -
 * subjects, class or house master roles, salary, payslips, leave, attendance or library loans - since that history has
 * to stay: such a person is deactivated instead. The confirmation is their full name typed out.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteTeacherPermanentlyUseCase {

    public record Result(String name, String account) {
    }

    private final TeacherRepository teacherRepo;
    private final TeacherPurgeRepository purgeRepo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "TEACHER_DELETED_PERMANENTLY")
    public Result execute(String schoolId, String teacherId, String confirmation) {
        var teacher = teacherRepo.findByIdAndSchoolId(teacherId, schoolId)
                .orElseThrow(() -> new NotFoundException("Teacher not found"));

        String name = (teacher.getUser().getGivenNames() + " " + teacher.getUser().getFamilyName()).trim();
        if (confirmation == null || !normalize(confirmation).equals(normalize(name))) {
            throw new RuleException("Type the full name (" + name + ") to confirm the deletion.");
        }

        var history = purgeRepo.history(teacherId);
        if (!history.isEmpty()) {
            String uses = history.entrySet().stream().map(e -> e.getValue() + " " + e.getKey())
                    .collect(Collectors.joining(", "));
            throw new RuleException(name + " has " + uses + " on record, so they can't be deleted. "
                    + "Deactivate them instead - that keeps their history.");
        }

        var account = purgeRepo.purge(schoolId, teacherId);
        logActivityUseCase.log(schoolId, ActivityType.TEACHER, "Teacher permanently deleted", name,
                "account=" + account.name(), null);
        return new Result(name, account.name());
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
