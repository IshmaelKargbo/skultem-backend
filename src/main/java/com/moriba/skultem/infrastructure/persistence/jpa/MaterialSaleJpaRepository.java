package com.moriba.skultem.infrastructure.persistence.jpa;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.model.MaterialSale.Status;
import com.moriba.skultem.infrastructure.persistence.entity.MaterialSaleEntity;

public interface MaterialSaleJpaRepository extends JpaRepository<MaterialSaleEntity, String> {

    Optional<MaterialSaleEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<MaterialSaleEntity> findAllByStudentIdAndSchoolIdOrderByCreatedAtDesc(String studentId, String schoolId,
            Pageable pageable);

    long countBySchoolId(String schoolId);

    long countBySchoolIdAndStatus(String schoolId, Status status);

    long countBySchoolIdAndStatusAndAmountPaidGreaterThan(String schoolId, Status status, BigDecimal amount);

    // Backs the sales list: search, status and the "paid, not collected" filter all combine
    // (rather than search silently overriding whichever status tab was active, as it used to when
    // each was a separate query the service picked between) - each condition is a no-op when its
    // param is null/false, so `filter` alone covers plain search, plain status, both together, or
    // neither. Student is optional on a sale (walk-in buyers have none) - a plain `join` would
    // silently drop every walk-in sale from both the search and the school-wide list.
    //
    // `pattern` arrives pre-wrapped with '%' from the adapter rather than built here via
    // concat('%', :query, '%'): with a null query, Postgres/JDBC can't pin a type on a bare bind
    // parameter that only ever appears inside a concat() argument, and falls back to bytea -
    // breaking the query even though the `:query is null` branch short-circuits it, because
    // Postgres resolves parameter types for the whole prepared statement up front. Binding an
    // unambiguous plain string (or null) straight into `like` sidesteps that entirely.
    @Query("""
                select s from MaterialSaleEntity s
                left join s.student st
                join s.material m
                where s.schoolId = :schoolId
                and (:pattern is null or (
                    lower(coalesce(st.givenNames, '')) like :pattern
                    or lower(coalesce(st.familyName, '')) like :pattern
                    or lower(coalesce(st.admissionNumber, '')) like :pattern
                    or lower(coalesce(s.customerName, '')) like :pattern
                    or lower(m.name) like :pattern
                ))
                and (:status is null or s.status = :status)
                and (:paidPending = false or (s.status = :pendingStatus and s.amountPaid > 0))
                order by s.createdAt desc
            """)
    Page<MaterialSaleEntity> filter(@Param("schoolId") String schoolId, @Param("pattern") String pattern,
            @Param("status") Status status, @Param("paidPending") boolean paidPending,
            @Param("pendingStatus") Status pendingStatus, Pageable pageable);

    @Query("""
                select coalesce(sum(s.amountPaid), 0) from MaterialSaleEntity s
                where s.schoolId = :schoolId and s.status <> :excluded
            """)
    BigDecimal sumAmountPaidBySchoolId(@Param("schoolId") String schoolId, @Param("excluded") Status excluded);

    @Query("""
                select coalesce(sum(s.totalAmount - s.amountPaid), 0) from MaterialSaleEntity s
                where s.schoolId = :schoolId and s.status <> :excluded
            """)
    BigDecimal sumOutstandingBalanceBySchoolId(@Param("schoolId") String schoolId, @Param("excluded") Status excluded);
}
