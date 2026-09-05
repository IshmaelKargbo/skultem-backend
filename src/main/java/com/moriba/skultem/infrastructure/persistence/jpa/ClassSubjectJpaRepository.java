package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.ClassSubjectEntity;

public interface ClassSubjectJpaRepository extends JpaRepository<ClassSubjectEntity, String> {
    boolean existsByClazz_IdAndSubject_Id(String classId, String subjectId);

    boolean existsByClazz_IdAndSubject_IdAndSchoolId(String classId, String subjectId, String schoolId);

    Optional<ClassSubjectEntity> findByClazz_IdAndSubject_IdAndSchoolId(String classId, String subjectId,
            String schoolId);

    @Modifying
    @Query("""
                update ClassSubjectEntity cs
                set cs.locked = true
                where cs.clazz.id = :classId
                and cs.subject.id = :subjectId
                and cs.schoolId = :schoolId
            """)
    void lockClassSubject(String classId, String subjectId, String schoolId);

    Page<ClassSubjectEntity> findAllByClazzIdAndSchoolId(String classId, String schoolId, Pageable pageable);

    Page<ClassSubjectEntity> findAllByClazzIdAndStreamIdAndSchoolId(String classId, String streamId, String schoolId, Pageable pageable);

    Page<ClassSubjectEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    Page<ClassSubjectEntity> findAllByClazz_Id(String classId, Pageable pageable);

    // classId/query are always real (possibly empty) strings, never null - a null String bound
    // into a lower(...) call leaves Postgres/the JDBC driver unable to infer its type from context
    // and it falls back to bytea ("function lower(bytea) does not exist"). See
    // ListClassSubjectBySchoolUseCase.
    @Query("""
                select cs from ClassSubjectEntity cs
                left join cs.stream st
                where cs.schoolId = :schoolId
                and (:classId = '' or cs.clazz.id = :classId)
                and (:mandatory is null or cs.mandatory = :mandatory)
                and (:query = ''
                     or lower(cs.clazz.name) like lower(concat('%', :query, '%'))
                     or lower(cs.subject.name) like lower(concat('%', :query, '%'))
                     or lower(st.name) like lower(concat('%', :query, '%')))
            """)
    Page<ClassSubjectEntity> search(
            @Param("schoolId") String schoolId,
            @Param("classId") String classId,
            @Param("mandatory") Boolean mandatory,
            @Param("query") String query,
            Pageable pageable);
}
