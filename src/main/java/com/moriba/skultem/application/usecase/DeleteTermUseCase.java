package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Only an UPCOMING term can be deleted - one that's ACTIVE or CLOSED already has real activity
 * (assessments, attendance, fees, ...) recorded against it.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class DeleteTermUseCase {

    private final TermRepository repo;

    @AuditLogAnnotation(action = "TERM_DELETED")
    public void execute(String schoolId, String id) {
        var term = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Term not found"));

        if (term.getStatus() != Term.Status.UPCOMING) {
            throw new RuleException("Only an upcoming term can be deleted");
        }

        repo.delete(term);
    }
}
