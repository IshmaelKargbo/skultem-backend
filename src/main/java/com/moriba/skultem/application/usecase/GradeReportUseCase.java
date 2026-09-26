package com.moriba.skultem.application.usecase;

import com.moriba.skultem.application.services.SectionScopeService;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentScoreDTO;
import com.moriba.skultem.application.dto.ReportBuilderDTO;
import com.moriba.skultem.application.mapper.AssessmentScoreMapper;
import com.moriba.skultem.domain.model.AssessmentScore;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.repository.AssessmentScoreRepository;
import com.moriba.skultem.domain.vo.Filter;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GradeReportUseCase {

    private final AssessmentScoreRepository repo;
    private final SectionScopeService sectionScopeService;
    private final ResolveScoreGradeUseCase resolveScoreGradeUseCase;
    private final com.moriba.skultem.domain.repository.AssessmentCaEntryRepository caEntryRepo;
    private final com.moriba.skultem.infrastructure.security.PermissionService permissionService;

    public Page<AssessmentScoreDTO> execute(ReportBuilderDTO request, int page, int size) {

        Pageable pageable = (size > 0) ? PageRequest.of(page - 1, size) : Pageable.unpaged();

        List<Filter> filters = request.filters();

        Page<AssessmentScore> res = repo.runReport(
                request.schoolId(),
                filters,
                sectionScopeService.levels(),
                pageable);

        // Continuous assessment (CA + formal test): the student and their parent see the breakdown behind a score - the
        // week-by-week CA and the formal test - once the teacher has submitted it (awaiting approval, shown as
        // provisional) or it is approved. Work still being recorded (draft) is not shown to them. School management
        // (admin, proprietor, owner) also sees it while it is being recorded, so they can monitor a class's progress
        // without waiting for the teacher to finish and submit everything.
        boolean management = permissionService.hasAnySchoolRole(request.schoolId(), "ADMIN", "PROPRIETOR", "OWNER");
        var released = res.getContent().stream()
                .filter(e -> e.getCycle().isContinuous()
                        && (e.isApproved() || e.isSubmited()
                                || (management && e.getStatus() != ClassSubjectAssessmentLifeCycle.Status.LOCKED)))
                .map(AssessmentScore::getId).toList();
        var entries = released.isEmpty()
                ? java.util.Map.<String, List<com.moriba.skultem.domain.model.AssessmentCaEntry>>of()
                : caEntryRepo.findAllByScoreIds(released).stream().collect(java.util.stream.Collectors
                        .groupingBy(com.moriba.skultem.domain.model.AssessmentCaEntry::getAssessmentScoreId));

        return res.map(e -> {
            var score = resolveScoreGradeUseCase.execute(request.schoolId(), e.getScore(), e.getStatus());
            return released.contains(e.getId())
                    ? AssessmentScoreMapper.toDTO(e, score, entries.getOrDefault(e.getId(), List.of()))
                    : AssessmentScoreMapper.toDTO(e, score);
        });
    }
}