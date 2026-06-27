package com.moriba.skultem.infrastructure.persistence.adapter;

import com.moriba.skultem.domain.model.SchemeOfWork;
import com.moriba.skultem.domain.repository.SchemeOfWorkRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.SchemeOfWorkJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.SchemeOfWorkMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SchemeOfWorkAdapter implements SchemeOfWorkRepository {
    private final SchemeOfWorkJpaRepository repo;

    @Override
    public void save(SchemeOfWork domain) {
        var entity = SchemeOfWorkMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<SchemeOfWork> findById(String id) {
        return repo.findById(id).map(SchemeOfWorkMapper::toDomain);
    }

    @Override
    public Optional<SchemeOfWork> findBySubjectAndTermAndSession(String subjectId, String termId, String sessionId) {
        return repo.findByTermIdAndSubjectIdAndSessionIdAndSchoolId(termId, subjectId, sessionId, sessionId)
                .map(SchemeOfWorkMapper::toDomain);
    }

    @Override
    public Page<SchemeOfWork> findAllBySessionId(String sessionId, Pageable pageable) {
        return repo.findAllBySessionId(sessionId, pageable).map(SchemeOfWorkMapper::toDomain);
    }

    @Override
    public boolean existsBySubjectAndTermAndSession(String subjectId, String termId, String sessionId) {
        return repo.existsByTermIdAndSubjectIdAndSessionIdAndSchoolId(termId, subjectId, sessionId, sessionId);
    }

}
