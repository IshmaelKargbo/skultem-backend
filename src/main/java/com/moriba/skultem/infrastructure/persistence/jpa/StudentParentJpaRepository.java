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

import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.StudentParentEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

public interface StudentParentJpaRepository extends JpaRepository<StudentParentEntity, String>, JpaSpecificationExecutor<StudentParentEntity>  {
    
    boolean existsByStudentIdAndParentIdAndSchoolId(String studentId, String parentId, String schoolId);

    Optional<StudentParentEntity> findByIdAndSchoolId(String id, String schoolId);

    Optional<StudentParentEntity> findByStudentIdAndSchoolId(String id, String schoolId);

    List<StudentParentEntity> findAllByStudentIdAndSchoolId(String id, String schoolId);

    Page<StudentParentEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    default Page<StudentParentEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
        Specification<StudentParentEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"), schoolId);

        if (filters != null && !filters.isEmpty()) {
            spec = spec.and(FilterSpecificationBuilder.build(filters));
        }

        return findAll(spec, pageable);
    }

    // Wipes a test school's roster/activity data (see WipeTestSchoolDataUseCase) -
    // config/setup tables are untouched, only this school's own rows here.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM StudentParentEntity e WHERE e.schoolId = :schoolId")
    void deleteAllBySchoolId(@Param("schoolId") String schoolId);
}
