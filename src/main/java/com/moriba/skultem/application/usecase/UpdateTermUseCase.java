package com.moriba.skultem.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.TermDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.application.mapper.TermMapper;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * A term's name/dates can only be edited while it's still UPCOMING - once it's ACTIVE, assessment
 * cycles and attendance are already running against its dates, and once it's CLOSED, it's history.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateTermUseCase {

    private final TermRepository repo;

    @AuditLogAnnotation(action = "TERM_UPDATED")
    public TermDTO execute(String schoolId, String id, String name, LocalDate startDate, LocalDate endDate) {
        var term = repo.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> new NotFoundException("Term not found"));

        if (term.getStatus() != Term.Status.UPCOMING) {
            throw new RuleException("Only an upcoming term can be edited");
        }

        if (!startDate.isBefore(endDate)) {
            throw new RuleException("Term start date must be before term end date");
        }

        var year = term.getAcademicYear();
        if (startDate.isBefore(year.getStartDate()) || endDate.isAfter(year.getEndDate())) {
            throw new RuleException("Term dates must fall within its academic year");
        }

        term.update(name, startDate, endDate);
        repo.save(term);

        return TermMapper.toDTO(term);
    }
}
