package com.moriba.skultem.application.usecase;

import java.util.ArrayList;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.SubjectRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Removes a subject from the school's catalog. A subject that is still in use is refused rather than removed, since
 * deleting it would take its class assignments, teacher assignments and students' enrolments with it.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteSubjectUseCase {
    private final SubjectRepository repo;
    private final JdbcTemplate jdbc;
    private final LogActivityUseCase logActivityUseCase;

    // Every table that points at a subject, with how to describe it to the person who has to clear it first.
    private record Usage(String table, String singular, String plural) {
    }

    private static final Usage[] USAGES = {
            new Usage("class_subjects", "class", "classes"),
            new Usage("stream_subjects", "stream", "streams"),
            new Usage("teacher_subjects", "teacher assignment", "teacher assignments"),
            new Usage("enrollment_subjcts", "student enrolment", "student enrolments"),
            new Usage("scheme_of_works", "scheme of work", "schemes of work")
    };

    @AuditLogAnnotation(action = "SUBJECT_DELETED")
    public void execute(String schoolId, String id) {
        var subject = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Subject not found"));

        var inUse = new ArrayList<String>();
        for (var usage : USAGES) {
            // Table names come from the constant above, never from input.
            Integer count = jdbc.queryForObject("SELECT count(*) FROM " + usage.table() + " WHERE subject_id = ?",
                    Integer.class, id);
            if (count != null && count > 0) {
                inUse.add(count + " " + (count == 1 ? usage.singular() : usage.plural()));
            }
        }
        if (!inUse.isEmpty()) {
            throw new RuleException(subject.getName() + " is still used by " + String.join(", ", inUse)
                    + " - remove those first before deleting it");
        }

        repo.delete(subject);

        logActivityUseCase.log(schoolId, ActivityType.SUBJECT, "Subject deleted",
                subject.getName() + " (" + subject.getCode() + ")", null, subject.getId());
    }
}
