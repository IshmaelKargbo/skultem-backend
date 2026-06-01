package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.AssessmentApprovalRequest;

public interface AssessmentApprovalRequestRepository {
    void save(AssessmentApprovalRequest domain);

    Page<AssessmentApprovalRequest> findAllByClassMasterSchoolId(String masterId, String schoolId, Pageable pageable);

    boolean existsByCycleAndTeacherSubject(String cycleId, String subjectId);

    Optional<AssessmentApprovalRequest> findByIdAndSchoolId(String id, String schoolId);

    Optional<AssessmentApprovalRequest> findByCycleAndTeacherSubject(String cycleId, String schoolId);
}
