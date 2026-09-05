package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.ParentEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

public interface ParentJpaRepository extends JpaRepository<ParentEntity, String>, JpaSpecificationExecutor<ParentEntity>  {
    boolean existsByPhoneAndSchoolId(String phone, String schoolId);

    Optional<ParentEntity> findByUser_IdAndSchoolId(String userId, String schoolId);

    Optional<ParentEntity> findByIdAndSchoolId(String id, String schoolId);

    // No baked-in ORDER BY - the caller's Pageable carries the Sort (see
    // ListParentBySchoolUseCase.resolveSort), and a fixed order here would either dominate or
    // conflict with it.
    Page<ParentEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    // Matches on name, email or phone - backs the parents list search box.
    @Query("""
                select p from ParentEntity p
                join p.user u
                where p.schoolId = :schoolId
                and (
                    lower(u.givenName) like lower(concat('%', :query, '%'))
                    or lower(u.familyName) like lower(concat('%', :query, '%'))
                    or lower(u.email) like lower(concat('%', :query, '%'))
                    or lower(p.phone) like lower(concat('%', :query, '%'))
                )
            """)
    Page<ParentEntity> search(@Param("schoolId") String schoolId, @Param("query") String query, Pageable pageable);

    default Page<ParentEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
        Specification<ParentEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"), schoolId);

        if (filters != null && !filters.isEmpty()) {
            spec = spec.and(FilterSpecificationBuilder.build(filters));
        }

        return findAll(spec, pageable);
    }
}
