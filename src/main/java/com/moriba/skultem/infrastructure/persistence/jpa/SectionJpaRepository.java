package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.SectionEntity;

public interface SectionJpaRepository extends JpaRepository<SectionEntity, String> {
    boolean existsByNameIgnoreCaseAndSchoolId(String name, String schoolId);

    Page<SectionEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Optional<SectionEntity> findByNameAndSchoolId(String name, String schoolId);

    Optional<SectionEntity> findByIdAndSchoolId(String id, String schoolId);

    // query is always a real (possibly empty) string, never null - a null String bound into a
    // lower(...) call leaves Postgres/the JDBC driver unable to infer its type from context and it
    // falls back to bytea ("function lower(bytea) does not exist"). See ListSectionBySchoolUseCase.
    @Query("""
                select s from SectionEntity s
                where s.schoolId = :schoolId
                and (:query = ''
                     or lower(s.name) like lower(concat('%', :query, '%'))
                     or lower(s.description) like lower(concat('%', :query, '%')))
                order by s.createdAt desc
            """)
    Page<SectionEntity> search(@Param("schoolId") String schoolId, @Param("query") String query, Pageable pageable);
}
