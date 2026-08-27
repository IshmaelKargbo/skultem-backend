package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.PromotionRequest;
import com.moriba.skultem.domain.repository.PromotionRequestRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.PromotionRequestJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.PromotionRequestMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PromotionRequestAdapter implements PromotionRequestRepository {
    private final PromotionRequestJpaRepository repo;

    private static final List<PromotionRequest.Status> OPEN_STATUSES = List.of(PromotionRequest.Status.PENDING_REVIEW,
            PromotionRequest.Status.RETURNED);

    @Override
    public void save(PromotionRequest domain) {
        var entity = PromotionRequestMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<PromotionRequest> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(PromotionRequestMapper::toDomain);
    }

    @Override
    public Optional<PromotionRequest> findOpenBySessionIdAndAcademicYearIdAndSchoolId(String sessionId,
            String academicYearId, String schoolId) {
        return repo
                .findFirstBySession_IdAndAcademicYear_IdAndSchoolIdAndStatusInOrderByCreatedAtDesc(sessionId,
                        academicYearId, schoolId, OPEN_STATUSES)
                .map(PromotionRequestMapper::toDomain);
    }

    @Override
    public Page<PromotionRequest> findBySchoolIdAndStatus(String schoolId, PromotionRequest.Status status,
            Pageable pageable) {
        return repo.findBySchoolIdAndStatusOrderByCreatedAtDesc(schoolId, status, pageable)
                .map(PromotionRequestMapper::toDomain);
    }

    @Override
    public Page<PromotionRequest> findBySchoolId(String schoolId, Pageable pageable) {
        return repo.findBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(PromotionRequestMapper::toDomain);
    }

    @Override
    public Page<PromotionRequest> findByMasterTeacherIdAndSchoolId(String teacherId, String schoolId,
            Pageable pageable) {
        return repo.findByMaster_Teacher_IdAndSchoolIdOrderByCreatedAtDesc(teacherId, schoolId, pageable)
                .map(PromotionRequestMapper::toDomain);
    }

    @Override
    public List<PromotionRequest> findByAcademicYearIdAndSchoolId(String academicYearId, String schoolId) {
        return repo.findByAcademicYear_IdAndSchoolId(academicYearId, schoolId).stream()
                .map(PromotionRequestMapper::toDomain)
                .toList();
    }
}
