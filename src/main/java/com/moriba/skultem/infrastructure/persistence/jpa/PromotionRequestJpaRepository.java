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

    // levels: the caller's management-section levels (every level for whole-school staff).
    Page<PromotionRequestEntity> findBySchoolIdAndStatusAndSession_Clazz_LevelInOrderByCreatedAtDesc(String schoolId,
            PromotionRequest.Status status, java.util.Collection<com.moriba.skultem.domain.vo.Level> levels, Pageable pageable);

    Page<PromotionRequestEntity> findBySchoolIdAndSession_Clazz_LevelInOrderByCreatedAtDesc(String schoolId,
            java.util.Collection<com.moriba.skultem.domain.vo.Level> levels, Pageable pageable);

    Page<PromotionRequestEntity> findByMaster_Teacher_IdAndSchoolIdOrderByCreatedAtDesc(String teacherId,
            String schoolId, Pageable pageable);

    List<PromotionRequestEntity> findByAcademicYear_IdAndSchoolId(String academicYearId, String schoolId);
}
