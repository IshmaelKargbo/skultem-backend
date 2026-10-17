package com.moriba.skultem.application.usecase;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.application.error.RuleException;
import com.moriba.skultem.domain.audit.AuditLogAnnotation;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.domain.repository.AssessmentTemplateRepository;
import com.moriba.skultem.domain.repository.EarlierTermAssessmentCleanupRepository;
import com.moriba.skultem.domain.repository.TermRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Sets the term the students of classes using a template start being graded in (SSS 1 joining in the second term). From
// then on they get no assessments for the earlier terms, and aren't billed for them. Assessments already created for
// those terms are cleared where nobody has marked or recorded anything; any with a mark in them stay.
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateAssessmentTemplateStartTermUseCase {

    private final AssessmentTemplateRepository templateRepo;
    private final AcademicYearRepository academicYearRepo;
    private final TermRepository termRepo;
    private final EarlierTermAssessmentCleanupRepository cleanupRepo;

    @AuditLogAnnotation(action = "ASSESSMENT_TEMPLATE_START_TERM_CHANGED")
    public int execute(String schoolId, String templateId, int termNumber) {
        var template = templateRepo.findByIdAndSchoolId(templateId, schoolId)
                .orElseThrow(() -> new NotFoundException("Assessment template not found"));

        var year = academicYearRepo.findActiveBySchool(schoolId).orElse(null);
        int terms = year == null ? 0 : termRepo.findByAcademicYearIdAndSchool(year.getId(), schoolId).size();
        if (termNumber < 1 || (terms > 0 && termNumber > terms)) {
            throw new RuleException("Choose a term from 1 to " + Math.max(terms, 1));
        }

        template.changeStartsInTerm(termNumber);
        templateRepo.save(template);

        if (year != null && termNumber > 1) {
            cleanupRepo.deleteUngradedBeforeTerm(schoolId, templateId, year.getId(), termNumber);
        }
        return termNumber;
    }
}
