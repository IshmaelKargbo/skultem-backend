package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.StreamSubjectEntity;

@Repository
public interface StreamSubjectJpaRepository extends JpaRepository<StreamSubjectEntity, String> {
    boolean existsByStreamIdAndSubjectId(String streamId, String subjectId);

    boolean existsByStreamIdAndSubjectIdAndSchoolId(String streamId, String subjectId, String schoolId);

    Page<StreamSubjectEntity> findAllByStreamId(String streamId, Pageable pageable);

    Page<StreamSubjectEntity> findAllBySchoolId(String schoolId, Pageable pageable);

    Optional<StreamSubjectEntity> findByStreamIdAndSubjectIdAndSchoolId(String streamId, String subjectId,
            String schoolId);

    Page<StreamSubjectEntity> findAllByStreamIdAndSchoolIdOrderByCreatedAtAsc(String streamId, String schoolId, Pageable pageable);
}
