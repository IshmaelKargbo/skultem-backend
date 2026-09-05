package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.SubjectEntity;

public interface SubjectJpaRepository extends JpaRepository<SubjectEntity, String> {
    boolean existsByCodeIgnoreCaseAndSchoolId(String code, String schoolId);

    Optional<SubjectEntity> findByIdAndSchoolId(String id, String schoolId);

    List<SubjectEntity> findAllByIdInAndSchoolId(Set<String> ids, String schoolId);

    // No baked-in ORDER BY - the caller's Pageable carries the Sort (see
    // ListSubjectBySchoolUseCase.resolveSort), and a fixed order here would either dominate or
    // conflict with it.
    Page<SubjectEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    // query is always a real (possibly empty) string, never null - a null String bound into a
    // lower(...) call leaves Postgres/the JDBC driver unable to infer its type from context and it
    // falls back to bytea ("function lower(bytea) does not exist"). See ListSubjectBySchoolUseCase.
    @Query("""
                select s from SubjectEntity s
                where s.schoolId = :schoolId
                and (:query = ''
                     or lower(s.name) like lower(concat('%', :query, '%'))
                     or lower(s.code) like lower(concat('%', :query, '%')))
            """)
    Page<SubjectEntity> search(@Param("schoolId") String schoolId, @Param("query") String query, Pageable pageable);
}
