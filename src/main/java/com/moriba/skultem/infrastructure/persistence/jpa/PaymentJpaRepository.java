package com.moriba.skultem.infrastructure.persistence.jpa;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.PaymentEntity;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, String> {

    Page<PaymentEntity> findAllByStudent_Id(String studentId, Pageable pageable);

    Page<PaymentEntity> findAllByFee_AcademicYear_IdAndSchoolIdOrderByCreatedAtDesc(String academicYearId, String school,
            Pageable pageable);

    @Query("""
                select coalesce(sum(p.amount), 0)
                from PaymentEntity p
                where p.student.id = :studentId
                and p.fee.id = :feeId
            """)
    BigDecimal sumPaymentsByStudentAndFee(
            @Param("studentId") String studentId,
            @Param("feeId") String feeId);

    @Query("""
                select coalesce(sum(p.amount), 0)
                from PaymentEntity p
                where p.student.id = :studentId
                and p.fee.academicYear.id = :academicYearId
            """)
    BigDecimal sumPaymentsByStudentThisYear(
            @Param("studentId") String studentId,
            @Param("academicYearId") String academicYearId);

    @Query("""
                select coalesce(sum(p.amount), 0)
                from PaymentEntity p
                where p.fee.id = :feeId
                and p.schoolId = :schoolId
            """)
    BigDecimal sumPaymentsByFeeAndSchool(
            @Param("feeId") String feeId,
            @Param("schoolId") String schoolId);

    @Query("""
                select coalesce(sum(p.amount), 0)
                from PaymentEntity p
                where p.schoolId = :schoolId
            """)
    BigDecimal sumPaymentsBySchool(
            @Param("schoolId") String schoolId);
}
