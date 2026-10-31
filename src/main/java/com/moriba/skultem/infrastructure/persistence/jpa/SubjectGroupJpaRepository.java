package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.SubjectGroupEntity;

public interface SubjectGroupJpaRepository extends JpaRepository<SubjectGroupEntity, String> {
    Page<SubjectGroupEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    // query/classId are always real (possibly empty) strings, never null - a null String bound
    // into a lower(...) call leaves Postgres/the JDBC driver unable to infer its type from context
    // and it falls back to bytea ("function lower(bytea) does not exist"). See
    // ListSubjectGroupBySchoolUseCase.
    @Query("""
                select g from SubjectGroupEntity g
                left join g.clazz c
                where g.schoolId = :schoolId
                and (:classId = '' or c.id = :classId)
                and ((c is null and :includeStreams = true) or c.level in :levels)
                and (:query = '' or lower(g.name) like lower(concat('%', :query, '%')))
            """)
    // Left join: stream-based groups (SSS) have no class, so an inner join on g.clazz would drop
    // them. They're included when the caller's scope covers a streamed level.
    Page<SubjectGroupEntity> search(
            @Param("schoolId") String schoolId,
            @Param("classId") String classId,
            @Param("query") String query,
            @Param("levels") java.util.Collection<com.moriba.skultem.domain.vo.Level> levels,
            @Param("includeStreams") boolean includeStreams,
            Pageable pageable);

    Page<SubjectGroupEntity> findAllByStream_IdAndSchoolIdOrderByClazz_LevelOrderAsc(String streamId, String schoolId, Pageable pageable);

    Page<SubjectGroupEntity> findAllByClazz_Id(String classId, Pageable pageable);

    Page<SubjectGroupEntity> findAllByClazz_IdAndSchoolIdOrderByClazz_LevelOrderAsc(String classId, String schoolId, Pageable pageable);

    Optional<SubjectGroupEntity> findByIdAndSchoolId(String id, String schoolId);

    Optional<SubjectGroupEntity> findByIdAndStream_IdAndSchoolId(String id, String streamId, String schoolid);

    Optional<SubjectGroupEntity> findByIdAndClazz_IdAndSchoolId(String id, String classId, String schoolid);

    List<SubjectGroupEntity> findAllByIdInAndSchoolId(Set<String> ids, String schoolId);

    List<SubjectGroupEntity> findAllByIdInAndStream_IdAndSchoolId(Set<String> ids, String streamId, String schoolId);
}
