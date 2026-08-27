package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.PromotionRequest;

public interface PromotionRequestRepository {
    void save(PromotionRequest domain);

    Optional<PromotionRequest> findByIdAndSchoolId(String id, String schoolId);

    /**
     * The most recent PENDING_REVIEW or RETURNED request for a session/year - used to guard against
     * duplicate submissions and to resume a returned one.
     */
    Optional<PromotionRequest> findOpenBySessionIdAndAcademicYearIdAndSchoolId(String sessionId,
            String academicYearId, String schoolId);

    Page<PromotionRequest> findBySchoolIdAndStatus(String schoolId, PromotionRequest.Status status,
            Pageable pageable);

    Page<PromotionRequest> findBySchoolId(String schoolId, Pageable pageable);

    Page<PromotionRequest> findByMasterTeacherIdAndSchoolId(String teacherId, String schoolId, Pageable pageable);

    List<PromotionRequest> findByAcademicYearIdAndSchoolId(String academicYearId, String schoolId);
}
