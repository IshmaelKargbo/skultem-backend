package com.moriba.skultem.application.usecase;

import java.util.ArrayList;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;
import com.moriba.skultem.domain.vo.ActivityType;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Removes a subject group. A group still holding class or stream subjects is refused, since those
 * rows point at it - the subjects have to be moved out of the group first.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteSubjectGroupUseCase {
    private final SubjectGroupRepository repo;
    private final JdbcTemplate jdbc;
    private final LogActivityUseCase logActivityUseCase;

    private record Usage(String table, String singular, String plural) {
    }

    private static final Usage[] USAGES = {
            new Usage("class_subjects", "class subject", "class subjects"),
            new Usage("stream_subjects", "stream subject", "stream subjects")
    };

    @AuditLogAnnotation(action = "SUBJECT_GROUP_DELETED")
    public void execute(String schoolId, String id) {
        var group = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Subject group not found"));

        var inUse = new ArrayList<String>();
        for (var usage : USAGES) {
            // Table names come from the constant above, never from input.
            Integer count = jdbc.queryForObject(
                    "SELECT count(*) FROM " + usage.table() + " WHERE subject_group_id = ?", Integer.class, id);
            if (count != null && count > 0) {
                inUse.add(count + " " + (count == 1 ? usage.singular() : usage.plural()));
            }
        }
        if (!inUse.isEmpty()) {
            throw new RuleException(group.getName() + " still has " + String.join(", ", inUse)
                    + " - remove them from the group before deleting it");
        }

        repo.delete(group);

        String scopeName = group.getStream() != null ? group.getStream().getName()
                : group.getClazz() != null ? group.getClazz().getName() : "";
        logActivityUseCase.log(schoolId, ActivityType.SUBJECT, "Subject group deleted",
                group.getName() + " - " + scopeName, null, group.getId());
    }
}
