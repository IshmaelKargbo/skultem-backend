package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.model.SchemeOfWork;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SchemeOfWorkRepository {
    void save(SchemeOfWork domain);

    Optional<SchemeOfWork> findById(String id);

    Optional<SchemeOfWork> findBySubjectAndTermAndSession(String subjectId, String termId, String sessionId);

    Page<SchemeOfWork> findAllBySessionId(String sessionId, Pageable pageable);

    boolean existsBySubjectAndTermAndSession(String subjectId, String termId, String sessionId);
}
