package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.LibraryCategoryEntity;

public interface LibraryCategoryJpaRepository extends JpaRepository<LibraryCategoryEntity, String> {
    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

    Optional<LibraryCategoryEntity> findByIdAndSchoolId(String id, String schoolId);

    long countBySchoolId(String schoolId);

    @Query("""
                select c from LibraryCategoryEntity c
                where c.schoolId = :schoolId
                and (
                    lower(c.name) like lower(concat('%', :query, '%'))
                    or lower(c.description) like lower(concat('%', :query, '%'))
                )
                order by c.name asc
            """)
    Page<LibraryCategoryEntity> search(@Param("schoolId") String schoolId, @Param("query") String query,
            Pageable pageable);
}
