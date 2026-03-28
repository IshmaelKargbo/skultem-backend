package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.TransactionEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

@Repository
public interface TransactionJpaRepository
                extends JpaRepository<TransactionEntity, String>, JpaSpecificationExecutor<TransactionEntity> {

        Optional<TransactionEntity> findTopBySchoolIdOrderByCreatedAtDesc(String schoolId);

        Page<TransactionEntity> findAllByAcademicYearIdAndSchoolId(String acadmicYearId, String schoolId,
                        Pageable pageable);

        default Page<TransactionEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
                Specification<TransactionEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"),
                                schoolId);

                if (filters != null && !filters.isEmpty()) {
                        spec = spec.and(FilterSpecificationBuilder.build(filters));
                }

                return findAll(spec, pageable);
        }
}
