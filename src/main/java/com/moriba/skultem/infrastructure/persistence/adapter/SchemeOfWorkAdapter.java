package com.moriba.skultem.infrastructure.persistence.adapter;

import com.moriba.skultem.domain.model.SchemeOfWork;
import com.moriba.skultem.domain.model.Week;
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
    public Optional<SchemeOfWork> findBySubjectAndTermAndSession(String subjectId, String termId, String sessionId, String schoolId) {
        return repo.findByTermIdAndSubjectIdAndSessionIdAndSchoolId(termId, subjectId, sessionId, schoolId)
                .map(SchemeOfWorkMapper::toDomain);
    }

    @Override
    public Page<SchemeOfWork> findAllBySchoolId(String school, Pageable pageable) {
        return repo.findAllBySchoolId(school, pageable).map(SchemeOfWorkMapper::toDomain);
    }

    @Override
    public boolean existsBySubjectAndTermAndSession(String subjectId, String termId, String sessionId, String schoolId) {
        return repo.existsByTermIdAndSubjectIdAndSessionIdAndSchoolId(termId, subjectId, sessionId, schoolId);
    }

    @Override
    public Page<SchemeOfWork> findAllByTeacherIdAndSchoolId(String teacherId, String school, Pageable pageable) {
        return repo.findAllByTeacherIdAndSchoolId(teacherId, school, pageable).map(SchemeOfWorkMapper::toDomain);
    }

    @Override
    public Page<SchemeOfWork> search(String school, String subjectId, String sessionId, String termId, Week.State progress, Pageable pageable) {
        return repo.search(school, subjectId, sessionId, termId,
                progress != null, progress == Week.State.COMPLETED, progress == Week.State.NOT_STARTED, progress == Week.State.IN_PROGRESS,
                pageable).map(SchemeOfWorkMapper::toDomain);
    }

    @Override
    public Page<SchemeOfWork> searchByTeacher(String teacherId, String school, String subjectId, String sessionId, String termId, Week.State progress, Pageable pageable) {
        return repo.searchByTeacher(teacherId, school, subjectId, sessionId, termId,
                progress != null, progress == Week.State.COMPLETED, progress == Week.State.NOT_STARTED, progress == Week.State.IN_PROGRESS,
                pageable).map(SchemeOfWorkMapper::toDomain);
    }

}
