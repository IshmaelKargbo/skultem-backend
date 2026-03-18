package com.moriba.skultem.infrastructure.persistence.jpa;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.StudentFeeEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

@Repository
public interface StudentFeeJpaRepository
        extends JpaRepository<StudentFeeEntity, String>, JpaSpecificationExecutor<StudentFeeEntity> {
    boolean existsByEnrollment_IdAndFee_IdAndStudent_IdAndSchoolId(String enrollmentId, String feeId,
            String studentId, String schoolId);

    @Query("""
                SELECT COALESCE(SUM(f.fee.amount), 0)
                FROM StudentFeeEntity f
                WHERE f.student.id = :studentId
                AND f.schoolId = :schoolId
            """)
    BigDecimal sumTotalFeeByStudent(String studentId, String schoolId);

    Page<StudentFeeEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    Optional<StudentFeeEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<StudentFeeEntity> findAllByStudent_IdAndSchoolId(String studentId, String schoolId, Pageable pageable);

    Page<StudentFeeEntity> findAllByEnrollment_IdAndStudent_IdAndSchoolId(String enrollmentId, String studentId,
            String schoolId, Pageable pageable);

    Page<StudentFeeEntity> findAllByEnrollment_IdAndSchoolId(String enrollmentId, String schoolId, Pageable pageable);

    Page<StudentFeeEntity> findAllByFee_IdAndSchoolId(String feeId, String schoolId, Pageable pageable);

    long countByFee_IdAndSchoolId(String feeId, String schoolId);

    Page<StudentFeeEntity> findAllByFee_IdAndEnrollment_IdAndSchoolId(String feeId, String enrollmentId,
            String schoolId, Pageable pageable);

    default Page<StudentFeeEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
        Specification<StudentFeeEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"), schoolId);

        if (filters != null && !filters.isEmpty()) {
            spec = spec.and(FilterSpecificationBuilder.build(filters));
        }

        return findAll(spec, pageable);
    }
}
