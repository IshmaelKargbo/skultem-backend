package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.infrastructure.persistence.entity.SchemeOfWorkEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchemeOfWorkJpaRepository extends JpaRepository<SchemeOfWorkEntity, String> {
    boolean existsByTermIdAndSubjectIdAndSessionIdAndSchoolId(String termId, String subjectId, String sessionId, String schoolId);

    Optional<SchemeOfWorkEntity> findByTermIdAndSubjectIdAndSessionIdAndSchoolId(String termId, String subjectId, String sessionId, String schoolId);

    Page<SchemeOfWorkEntity> findAllBySchoolId(String school, Pageable pageable);
}
