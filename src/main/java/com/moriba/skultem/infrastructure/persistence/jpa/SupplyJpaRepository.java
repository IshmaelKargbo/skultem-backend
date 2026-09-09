package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.SupplyEntity;

public interface SupplyJpaRepository extends JpaRepository<SupplyEntity, String> {
    Page<SupplyEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Page<SupplyEntity> findAllByStudentIdAndSchoolIdOrderByCreatedAtDesc(String studentId, String schoolId,
            Pageable pageable);

    boolean existsByStudentIdAndMaterialIdAndSchoolId(String studentId, String materialId, String schoolId);

    Optional<SupplyEntity> findByIdAndSchoolId(String id, String schoolId);

    // Matches on student name/admission number or material name - backs the supply list's search box.
    @Query("""
                select s from SupplyEntity s
                join s.student st
                join s.material m
                where s.schoolId = :schoolId
                and (
                    lower(st.givenNames) like lower(concat('%', :query, '%'))
                    or lower(st.familyName) like lower(concat('%', :query, '%'))
                    or lower(st.admissionNumber) like lower(concat('%', :query, '%'))
                    or lower(m.name) like lower(concat('%', :query, '%'))
                )
                order by s.createdAt desc
            """)
    Page<SupplyEntity> search(@Param("schoolId") String schoolId, @Param("query") String query, Pageable pageable);
}
