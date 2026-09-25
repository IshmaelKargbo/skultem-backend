package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.vo.Level;

import java.util.Collection;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.AssessmentApprovalRequest;

public interface AssessmentApprovalRequestRepository {
    void save(AssessmentApprovalRequest domain);

    // levels: only requests for classes at these levels (see SectionScope) - on every list/count here.
    Page<AssessmentApprovalRequest> findAllByClassMasterSchoolId(String masterId, String schoolId,
            AssessmentApprovalRequest.Status status, String query, Collection<Level> levels, Pageable pageable);

    long countByClassMasterSchoolIdAndStatus(String masterId, String schoolId, AssessmentApprovalRequest.Status status,
            Collection<Level> levels);

    // School-wide, unlike the two above (which are scoped to one class master's teacher id) -
    // backs the admin approval view's default list.
    Page<AssessmentApprovalRequest> findAllBySchool(String schoolId, String academicYearId,
            AssessmentApprovalRequest.Status status, String query, Collection<Level> levels, Pageable pageable);

    long countBySchoolAndStatus(String schoolId, String academicYearId, AssessmentApprovalRequest.Status status,
            Collection<Level> levels);

    boolean existsByCycleAndTeacherSubject(String cycleId, String subjectId);

    Optional<AssessmentApprovalRequest> findByIdAndSchoolId(String id, String schoolId);

    Optional<AssessmentApprovalRequest> findByCycleAndTeacherSubject(String cycleId, String schoolId);

    // A cycle has at most one live approval request regardless of which of a subject's (possibly
    // several) teachers submits it - looking this up per-teacherSubjectId would let two co-teachers
    // each spawn their own request for the same shared cycle.
    Optional<AssessmentApprovalRequest> findByCycle(String cycleId);
}
