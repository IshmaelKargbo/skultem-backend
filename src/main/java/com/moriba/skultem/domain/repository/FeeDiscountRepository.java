package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.FeeDiscount;

public interface FeeDiscountRepository {
    void save(FeeDiscount domain);

    Optional<FeeDiscount> findByIdAndSchoolId(String id, String schoolId);

    boolean existsByNameAndEnrollmentAndFeeAndSchoolId(String name, String enrollmentId, String feeId, String schoolId);

    Page<FeeDiscount> findBySchoolAndEnrollment(String schoolId, String enrollmentId, Pageable pageable);

    Page<FeeDiscount> findAllBySchool(String schoolId, Pageable pageable);

    List<FeeDiscount> findBySchoolAndStudentIdAndFeeId(String schoolId, String studentId, String feeId);
}
