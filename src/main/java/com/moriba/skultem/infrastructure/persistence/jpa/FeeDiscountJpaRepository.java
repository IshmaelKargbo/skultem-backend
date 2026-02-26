package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.FeeDiscountEntity;

@Repository
public interface FeeDiscountJpaRepository extends JpaRepository<FeeDiscountEntity, String> {
        boolean existsByNameIgnoreCaseAndEnrollment_IdAndSchoolId(String name, String enrollmentId, String schoolId);
        
        boolean existsByNameIgnoreCaseAndEnrollment_IdAndFee_IdAndSchoolId(
                        String name,
                        String enrollmentId,
                        String feeId,
                        String schoolId);

        Page<FeeDiscountEntity> findAllBySchoolId(String schoolId, Pageable pageable);

        Optional<FeeDiscountEntity> findAllByIdAndSchoolId(String id, String schoolId);

        Page<FeeDiscountEntity> findAllByEnrollment_IdAndSchoolIdOrderByCreatedAtDesc(String enrollmentId,
                        String schoolId, Pageable pageable);

        List<FeeDiscountEntity> findAllByEnrollment_Student_IdAndSchoolIdAndFee_IdOrderByCreatedAtDesc(String studentId,
                        String schoolId, String feeId);

        Page<FeeDiscountEntity> findAllByFee_IdAndSchoolIdOrderByCreatedAtDesc(String feeId,
                        String schoolId, Pageable pageable);
}
