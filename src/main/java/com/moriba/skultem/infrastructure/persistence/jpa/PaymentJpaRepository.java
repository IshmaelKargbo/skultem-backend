package com.moriba.skultem.infrastructure.persistence.jpa;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.FeeCategoryRevenue;
import com.moriba.skultem.infrastructure.persistence.entity.PaymentEntity;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, String> {

        Page<PaymentEntity> findAllByStudent_Id(String studentId, Pageable pageable);

        Page<PaymentEntity> findAllByFee_AcademicYear_IdAndSchoolIdOrderByCreatedAtDesc(
                        String academicYearId,
                        String schoolId,
                        Pageable pageable);

        @Query("""
                        select coalesce(sum(p.amount), 0)
                        from PaymentEntity p
                        where p.student.id = :studentId
                        and p.fee.id = :feeId
                        """)
        BigDecimal sumPaymentsByStudentAndFee(String studentId, String feeId);

        @Query("""
                        select coalesce(sum(p.amount), 0)
                        from PaymentEntity p
                        where p.student.id = :studentId
                        and p.fee.academicYear.id = :academicYearId
                        """)
        BigDecimal sumPaymentsByStudentThisYear(String studentId, String academicYearId);

        @Query("""
                        select coalesce(sum(p.amount), 0)
                        from PaymentEntity p
                        where p.fee.id = :feeId
                        and p.schoolId = :schoolId
                        """)
        BigDecimal sumPaymentsByFeeAndSchool(String feeId, String schoolId);

        @Query("""
                        select coalesce(sum(p.amount), 0)
                        from PaymentEntity p
                        where p.schoolId = :schoolId
                        """)
        BigDecimal sumPaymentsBySchool(String schoolId);

        @Query("""
                        select coalesce(sum(p.amount), 0)
                        from PaymentEntity p
                        where p.schoolId = :schoolId
                        and p.createdAt between :start and :end
                        """)
        BigDecimal sumPaymentsBySchoolAndDateRange(String schoolId, Instant start, Instant end);

        @Query("""
                        SELECT new com.moriba.skultem.domain.model.FeeCategoryRevenue(
                            f.category.name,
                            SUM(p.amount)
                        )
                        FROM PaymentEntity p
                        JOIN p.fee f
                        WHERE p.schoolId = :schoolId
                        GROUP BY f.category.name
                        """)
        List<FeeCategoryRevenue> sumRevenueByCategory(String schoolId);
}