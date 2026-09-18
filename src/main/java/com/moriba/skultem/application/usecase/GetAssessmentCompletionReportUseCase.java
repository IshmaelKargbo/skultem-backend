package com.moriba.skultem.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentCompletionReportDTO;
import com.moriba.skultem.application.dto.AssessmentCompletionRowDTO;
import com.moriba.skultem.application.error.NotFoundException;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.repository.ClassRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.domain.vo.Level;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// Assessment Completion Report - a flat projection of real ClassSubjectAssessmentLifeCycle status
// rows (no invented "expected assessment count"; see
// ClassSubjectAssessmentLifeCycleRepository#completionReportRows for why this uses a dedicated
// projection query rather than the lazy-loaded domain objects other use cases work with).
// classId/subjectId nullable = every class/subject for the term. A whole-school, all-terms-open
// view can run into the hundreds of rows (classes x subjects x assessments), so this paginates
// the same way GetClassAcademicPerformanceUseCase paginates its student list.
@Service
@Transactional
@RequiredArgsConstructor
public class GetAssessmentCompletionReportUseCase {

    private final ClassSubjectAssessmentLifeCycleRepository cycleRepo;
    private final ClassRepository classRepo;
    private final TermRepository termRepo;
    private final ResolveAcademicYearUseCase resolveAcademicYearUseCase;

    public AssessmentCompletionReportDTO execute(String schoolId, String classId, String subjectId,
            String academicYearId, String termId, Level level, int page, int size) {
        var academicYear = resolveAcademicYearUseCase.execute(schoolId, academicYearId);
        var term = resolveTerm(schoolId, academicYear.getId(), termId);

        var rows = cycleRepo.completionReportRows(schoolId, term.getId(), classId, subjectId);

        Map<String, Level> levelByClass = new HashMap<>();
        if (level != null) {
            for (var clazz : classRepo.findBySchool(schoolId, Pageable.unpaged()).getContent()) {
                levelByClass.put(clazz.getId(), clazz.getLevel());
            }
        }

        List<AssessmentCompletionRowDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            String rowClassId = (String) row[0];
            if (level != null && !level.equals(levelByClass.get(rowClassId))) {
                continue;
            }

            result.add(new AssessmentCompletionRowDTO(
                    rowClassId,
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    (String) row[5],
                    (ClassSubjectAssessmentLifeCycle.Status) row[7]));
        }

        // Sort deterministically before paginating so the same page is returned consistently
        // across requests rather than depending on query/iteration order.
        result.sort(Comparator
                .comparing(AssessmentCompletionRowDTO::className, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AssessmentCompletionRowDTO::subjectName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AssessmentCompletionRowDTO::assessmentName, String.CASE_INSENSITIVE_ORDER));

        int total = result.size();
        int safeSize = size > 0 ? size : total;
        int safePage = Math.max(page, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);

        return new AssessmentCompletionReportDTO(result.subList(fromIndex, toIndex), safePage, safeSize, total);
    }

    private Term resolveTerm(String schoolId, String academicYearId, String termId) {
        if (termId != null && !termId.isBlank()) {
            return termRepo.findByIdAndAcademicYearIdAndSchoolId(termId, academicYearId, schoolId)
                    .orElseThrow(() -> new NotFoundException("Term not found"));
        }

        return termRepo.findActiveBySchoolAndAcademicYear(schoolId, academicYearId)
                .orElseThrow(() -> new NotFoundException("Active term not found"));
    }
}
