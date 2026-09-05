package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.StreamSubjectEntity;

public interface StreamSubjectJpaRepository extends JpaRepository<StreamSubjectEntity, String> {
    boolean existsByStreamIdAndSubjectId(String streamId, String subjectId);

    boolean existsByStreamIdAndSubjectIdAndSchoolId(String streamId, String subjectId, String schoolId);

    Page<StreamSubjectEntity> findAllByStreamId(String streamId, Pageable pageable);

    @Modifying
    @Query("""
                update ClassSubjectEntity cs
                set cs.locked = true
                where cs.clazz.id = :classId
                and cs.subject.id = :subjectId
                and cs.schoolId = :schoolId
            """)
    void lockClassSubject(String classId, String subjectId, String schoolId);

    Page<StreamSubjectEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    Optional<StreamSubjectEntity> findByStreamIdAndSubjectIdAndSchoolId(String streamId, String subjectId,
            String schoolId);

    Page<StreamSubjectEntity> findAllByStreamIdAndSchoolIdOrderByCreatedAtAsc(String streamId, String schoolId, Pageable pageable);

    // streamId/query are always real (possibly empty) strings, never null - a null String bound
    // into a lower(...) call leaves Postgres/the JDBC driver unable to infer its type from context
    // and it falls back to bytea ("function lower(bytea) does not exist"). See
    // ListStreamSubjectBySchoolUseCase.
    @Query("""
                select ss from StreamSubjectEntity ss
                where ss.schoolId = :schoolId
                and (:streamId = '' or ss.stream.id = :streamId)
                and (:query = ''
                     or lower(ss.stream.name) like lower(concat('%', :query, '%'))
                     or lower(ss.subject.name) like lower(concat('%', :query, '%')))
            """)
    Page<StreamSubjectEntity> search(
            @Param("schoolId") String schoolId,
            @Param("streamId") String streamId,
            @Param("query") String query,
            Pageable pageable);
}
