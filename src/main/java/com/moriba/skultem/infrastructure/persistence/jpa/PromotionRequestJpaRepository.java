package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.moriba.skultem.domain.model.PromotionRequest;
import com.moriba.skultem.infrastructure.persistence.entity.PromotionRequestEntity;

public interface PromotionRequestJpaRepository extends JpaRepository<PromotionRequestEntity, String> {
    Optional<PromotionRequestEntity> findByIdAndSchoolId(String id, String schoolId);

    Optional<PromotionRequestEntity> findFirstBySession_IdAndAcademicYear_IdAndSchoolIdAndStatusInOrderByCreatedAtDesc(
            String sessionId, String academicYearId, String schoolId, List<PromotionRequest.Status> statuses);

    Page<PromotionRequestEntity> findBySchoolIdAndStatusOrderByCreatedAtDesc(String schoolId,
            PromotionRequest.Status status, Pageable pageable);

    Page<PromotionRequestEntity> findBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Page<PromotionRequestEntity> findByMaster_Teacher_IdAndSchoolIdOrderByCreatedAtDesc(String teacherId,
            String schoolId, Pageable pageable);

    List<PromotionRequestEntity> findByAcademicYear_IdAndSchoolId(String academicYearId, String schoolId);
}
