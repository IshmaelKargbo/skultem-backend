package com.moriba.skultem.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.moriba.skultem.application.dto.AssessmentCycleDTO;
import com.moriba.skultem.application.dto.AssessmentDTO;
import com.moriba.skultem.application.mapper.AssessmentCycleMapper;
import com.moriba.skultem.application.mapper.AssessmentMapper;
import com.moriba.skultem.domain.model.AssessmentApprovalRequest;
import com.moriba.skultem.domain.model.ClassSubjectAssessmentLifeCycle;
import com.moriba.skultem.domain.repository.AssessmentApprovalRequestRepository;
import com.moriba.skultem.domain.repository.AssessmentRepository;
import com.moriba.skultem.domain.repository.ClassSubjectAssessmentLifeCycleRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ListAssessmentUseCase {

        private final ClassSubjectAssessmentLifeCycleRepository repo;
        private final AssessmentRepository assessmentRepo;
        private final AssessmentApprovalRequestRepository approvalRepo;

        public List<AssessmentCycleDTO> execute(String schoolId, String subjectId, String termId) {
                return repo.findAllBySubjectAndTerm(subjectId, termId).stream()
                                .map(cycle -> AssessmentCycleMapper.toDTO(cycle, returnReasonFor(cycle)))
                                .toList();
        }

        // An assessment that was sent back carries the approver's note, so whoever grades it sees what to
        // fix instead of just a "Returned" status. Only while it's actually still returned - once it's
        // resubmitted the request is pending again and the old note no longer applies.
        private String returnReasonFor(ClassSubjectAssessmentLifeCycle cycle) {
                if (cycle.getStatus() != ClassSubjectAssessmentLifeCycle.Status.RETURNED) {
                        return null;
                }

                return approvalRepo.findByCycle(cycle.getId())
                                .filter(request -> request.getStatus() == AssessmentApprovalRequest.Status.RETURNED)
                                .map(AssessmentApprovalRequest::getReturnReason)
                                .orElse(null);
        }

        public List<AssessmentDTO> executeAssessment(String schoolId) {
                return assessmentRepo.findAllBySchoolId(schoolId).stream()
                                .map(AssessmentMapper::toDTO)
                                .toList();
        }
}
