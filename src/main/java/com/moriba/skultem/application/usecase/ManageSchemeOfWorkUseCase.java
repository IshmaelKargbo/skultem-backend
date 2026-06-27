package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.model.SchemeOfWork;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.domain.repository.SchemeOfWorkRepository;
import com.moriba.skultem.domain.repository.SubjectRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.vo.ActivityType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageSchemeOfWorkUseCase {

    private final TermRepository termRepo;
    private final ClassSessionRepository sessionRepo;
    private final SubjectRepository subjectRepo;
    private final SchemeOfWorkRepository repo;
    private final LogActivityUseCase logActivityUseCase;

    @AuditLogAnnotation(action = "SCHEME_OF_WORK_CREATED")
    public SchemeOfWork execute(String schoolId, String subjectId, String sessionId, String termId) {
        return create(schoolId, subjectId, sessionId, termId);
    }

    private SchemeOfWork create(String schoolId, String subjectId, String sessionId, String termId) {

        var term = termRepo.findById(termId)
                .orElseThrow(() -> new NotFoundException("No term found"));

        var subject = subjectRepo.findById(termId)
                .orElseThrow(() -> new NotFoundException("No subject found"));

        var session = sessionRepo.findByIdAndSchoolId(sessionId, schoolId)
                .orElseThrow(() -> new NotFoundException("No class found"));

        String id = UUID.randomUUID().toString();

        var weeks = getWeeks(term.getStartDate(), term.getEndDate());

        var domain = SchemeOfWork.create(id, schoolId, subject, session, term, weeks);

        repo.save(domain);

        logActivityUseCase.log(
                schoolId,
                ActivityType.SCHOOL,
                subject.getName() + " scheme created",
                subject.getName(),
                null,
                domain.getId());

        return domain;
    }

    public long getWeeks(LocalDate startDate, LocalDate endDate) {
        long weeks = ChronoUnit.WEEKS.between(startDate, endDate);
        return weeks;
    }
}
