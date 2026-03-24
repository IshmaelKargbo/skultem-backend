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
import com.moriba.skultem.infrastructure.persistence.entity.ParentEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

@Repository
public interface ParentJpaRepository extends JpaRepository<ParentEntity, String>, JpaSpecificationExecutor<ParentEntity>  {
    boolean existsByPhoneAndSchoolId(String phone, String schoolId);

    Optional<ParentEntity> findByUser_IdAndSchoolId(String userId, String schoolId);

    Optional<ParentEntity> findByIdAndSchoolId(String id, String schoolId);

    Page<ParentEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    default Page<ParentEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
        Specification<ParentEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"), schoolId);

        if (filters != null && !filters.isEmpty()) {
            spec = spec.and(FilterSpecificationBuilder.build(filters));
        }

        return findAll(spec, pageable);
    }
}
