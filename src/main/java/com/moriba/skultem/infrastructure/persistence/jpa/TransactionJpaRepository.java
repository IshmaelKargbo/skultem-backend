package com.moriba.skultem.infrastructure.persistence.jpa;

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

import com.moriba.skultem.domain.model.Transaction.ReferenceType;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.TransactionEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

public interface TransactionJpaRepository
                extends JpaRepository<TransactionEntity, String>, JpaSpecificationExecutor<TransactionEntity> {

        Optional<TransactionEntity> findTopBySchoolIdOrderByCreatedAtDesc(String schoolId);

        Page<TransactionEntity> findAllByAcademicYearIdAndSchoolId(String acadmicYearId, String schoolId,
                        Pageable pageable);

        /**
         * The school's transactions narrowed by any of: type, direction (money in/out), what they
         * relate to, and a date range - each ignored when null. {@code from} is inclusive and
         * {@code toExclusive} exclusive, so a whole last day is included by passing the next midnight.
         */
        default Page<TransactionEntity> searchTransactions(String schoolId, String academicYearId,
                        com.moriba.skultem.domain.model.Transaction.TransactionType type,
                        com.moriba.skultem.domain.model.Transaction.Direction direction,
                        com.moriba.skultem.domain.model.Transaction.ReferenceType referenceType,
                        java.time.Instant from, java.time.Instant toExclusive, Pageable pageable) {
                // Always one academic year - never every year's transactions at once.
                Specification<TransactionEntity> spec = (root, query, cb) -> cb.and(
                                cb.equal(root.get("schoolId"), schoolId),
                                cb.equal(root.get("academicYear").get("id"), academicYearId));

                if (type != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("transactionType"), type));
                }
                if (direction != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("direction"), direction));
                }
                if (referenceType != null) {
                        spec = spec.and((root, query, cb) -> cb.equal(root.get("referenceType"), referenceType));
                }
                if (from != null) {
                        spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
                }
                if (toExclusive != null) {
                        spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), toExclusive));
                }

                return findAll(spec, pageable);
        }

        default Page<TransactionEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
                Specification<TransactionEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"),
                                schoolId);

                if (filters != null && !filters.isEmpty()) {
                        spec = spec.and(FilterSpecificationBuilder.build(filters));
                }

                return findAll(spec, pageable);
        }

        // Wipes a test school's STUDENT transactions only (see WipeTestSchoolDataUseCase) - expense,
        // payroll and material-sale history for the school isn't roster/activity data and stays put.
        @Modifying(flushAutomatically = true, clearAutomatically = true)
        @Query("DELETE FROM TransactionEntity e WHERE e.schoolId = :schoolId AND e.referenceType = :referenceType")
        void deleteAllBySchoolIdAndReferenceType(@Param("schoolId") String schoolId,
                        @Param("referenceType") ReferenceType referenceType);
}
