package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.AssessmentApprovalRequest;

public interface AssessmentApprovalRequestRepository {
    void save(AssessmentApprovalRequest domain);

    Page<AssessmentApprovalRequest> findAllByClassMasterSchoolId(String masterId, String schoolId,
            AssessmentApprovalRequest.Status status, String query, Pageable pageable);

    long countByClassMasterSchoolIdAndStatus(String masterId, String schoolId, AssessmentApprovalRequest.Status status);

    // School-wide, unlike the two above (which are scoped to one class master's teacher id) -
    // backs the admin approval view's default list.
    Page<AssessmentApprovalRequest> findAllBySchool(String schoolId, String academicYearId,
            AssessmentApprovalRequest.Status status, String query, Pageable pageable);

    long countBySchoolAndStatus(String schoolId, String academicYearId, AssessmentApprovalRequest.Status status);

    boolean existsByCycleAndTeacherSubject(String cycleId, String subjectId);

    Optional<AssessmentApprovalRequest> findByIdAndSchoolId(String id, String schoolId);

    Optional<AssessmentApprovalRequest> findByCycleAndTeacherSubject(String cycleId, String schoolId);
}
