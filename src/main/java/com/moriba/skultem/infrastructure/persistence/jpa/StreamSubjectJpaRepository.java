package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.StreamSubjectEntity;

@Repository
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
}
