package com.moriba.skultem.infrastructure.persistence.jpa;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.StudentFeeEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

public interface StudentFeeJpaRepository
        extends JpaRepository<StudentFeeEntity, String>, JpaSpecificationExecutor<StudentFeeEntity> {
    boolean existsByEnrollment_IdAndFee_IdAndStudent_IdAndSchoolId(String enrollmentId, String feeId,
            String studentId, String schoolId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM StudentFeeEntity f WHERE f.enrollment.id = :enrollmentId AND f.schoolId = :schoolId")
    void deleteAllByEnrollmentAndSchool(@Param("enrollmentId") String enrollmentId,
            @Param("schoolId") String schoolId);

    @Query("""
                SELECT COALESCE(SUM(f.fee.amount), 0)
                FROM StudentFeeEntity f
                WHERE f.student.id = :studentId
                AND f.schoolId = :schoolId
            """)
    BigDecimal sumTotalFeeByStudent(String studentId, String schoolId);

    Page<StudentFeeEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    Optional<StudentFeeEntity> findByIdAndSchoolId(String id, String schoolId);

    // A student's fees for one academic year - a fee structure belongs to a year, so this is the fees
    // charged for that year only.
    Page<StudentFeeEntity> findAllByStudent_IdAndFee_AcademicYear_IdAndSchoolId(String studentId,
            String academicYearId, String schoolId, Pageable pageable);

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

    // Every charge a school made to its students for one academic year (optionally narrowed to one
    // term) - the platform fee (fee.system = true) is deliberately excluded, since it isn't the
    // school's own fee revenue. Backs the Fees Reporting dashboard/term-summary/student-balances/
    // outstanding reports: fetch-joined so mapping every row in Java (amount, discount, class,
    // student) never triggers a lazy-load per row.
    @Query("""
                select f from StudentFeeEntity f
                join fetch f.fee fs
                join fetch fs.category
                join fetch fs.term
                join fetch f.enrollment e
                join fetch e.clazz
                join fetch e.section
                left join fetch e.stream
                join fetch f.student
                left join fetch f.discount
                where f.schoolId = :schoolId
                and fs.academicYear.id = :academicYearId
                and fs.system = false
                and (:termId = '' or fs.term.id = :termId)
            """)
    List<StudentFeeEntity> findSchoolFeeRows(@Param("schoolId") String schoolId,
            @Param("academicYearId") String academicYearId, @Param("termId") String termId);

    // Wipes a test school's roster/activity data (see WipeTestSchoolDataUseCase) -
    // config/setup tables are untouched, only this school's own rows here.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM StudentFeeEntity e WHERE e.schoolId = :schoolId")
    void deleteAllBySchoolId(@Param("schoolId") String schoolId);
}
