package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.FeeDiscount;
import com.moriba.skultem.domain.repository.FeeDiscountRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.FeeDiscountJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.FeeDiscountMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FeeDiscountAdapter implements FeeDiscountRepository {
    private final FeeDiscountJpaRepository repo;

    @Override
    public void save(FeeDiscount domain) {
        var entity = FeeDiscountMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<FeeDiscount> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findAllByIdAndSchoolId(id, schoolId).map(FeeDiscountMapper::toDomain);
    }

    @Override
    public boolean existsByNameAndEnrollmentAndFeeAndSchoolId(String name, String enrollmentId, String feeId,
            String schoolId) {
        return repo.existsByNameIgnoreCaseAndEnrollment_IdAndFee_IdAndSchoolId(name, enrollmentId, feeId, schoolId);
    }

    @Override
    public Page<FeeDiscount> findBySchoolAndEnrollment(String schoolId, String enrollmentId,
            Pageable pageable) {
        return repo.findAllByEnrollment_IdAndSchoolIdOrderByCreatedAtDesc(enrollmentId, schoolId, pageable)
                .map(FeeDiscountMapper::toDomain);
    }

    @Override
    public List<FeeDiscount> findBySchoolAndStudentIdAndFeeId(String schoolId, String studentId,
            String feeId) {
        return repo.findAllByEnrollment_Student_IdAndSchoolIdAndFee_IdOrderByCreatedAtDesc(studentId, schoolId, feeId).stream()
                .map(FeeDiscountMapper::toDomain).toList();
    }

    @Override
    public Page<FeeDiscount> findAllBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(FeeDiscountMapper::toDomain);
    }
}
