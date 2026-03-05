package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import com.moriba.skultem.domain.model.AssessmentApprovalRequest;

public interface AssessmentApprovalRequestRepository {
    void save(AssessmentApprovalRequest domain);

    List<AssessmentApprovalRequest> findAllByClassMasterSchoolId(String masterId, String schoolId);

    boolean existsByCycleAndTeacherSubject(String cycleId, String subjectId);

    Optional<AssessmentApprovalRequest> findByIdAndSchoolId(String id, String schoolId);

    Optional<AssessmentApprovalRequest> findByCycleAndTeacherSubject(String cycleId, String schoolId);
}
