package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.StreamSubject;
import com.moriba.skultem.domain.repository.StreamSubjectRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.StreamSubjectJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.StreamSubjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StreamSubjectAdapter implements StreamSubjectRepository {
    private final StreamSubjectJpaRepository repo;

    @Override
    public void save(StreamSubject domain) {
        var entity = StreamSubjectMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public void delete(StreamSubject domain) {
        var entity = StreamSubjectMapper.toEntity(domain);
        repo.delete(entity);
    }

    @Override
    public Optional<StreamSubject> findById(String id) {
        return repo.findById(id).map(StreamSubjectMapper::toDomain);
    }

    @Override
    public boolean existsByStreamAndSubject(String streamId, String subjectId) {
        return repo.existsByStreamIdAndSubjectId(streamId, subjectId);
    }

    @Override
    public Page<StreamSubject> findByStream(String streamId, Pageable pageable) {
        return repo.findAllByStreamId(streamId, pageable).map(StreamSubjectMapper::toDomain);
    }

    @Override
    public boolean existsByStreamIdAndSubjectIdAndSchoolId(String streamId, String subjectId, String schoolId) {
        return repo.existsByStreamIdAndSubjectIdAndSchoolId(streamId, subjectId, schoolId);
    }

    @Override
    public Page<StreamSubject> findBySchoolId(String school, Pageable pageable) {
        return repo.findAllBySchoolId(school, pageable).map(StreamSubjectMapper::toDomain);
    }

    @Override
    public Optional<StreamSubject> findByStreamIdAndSubjectIdAndSchoolId(String streamId, String subjectId,
            String schoolId) {
        return repo.findByStreamIdAndSubjectIdAndSchoolId(streamId, subjectId, schoolId)
                .map(StreamSubjectMapper::toDomain);
    }

    @Override
    public Page<StreamSubject> findAllByStreamIdAndSchoolId(String streamId, String schoolId, Pageable pageable) {
        return repo.findAllByStreamIdAndSchoolIdOrderByCreatedAtAsc(streamId, schoolId, pageable)
                .map(StreamSubjectMapper::toDomain);
    }
}
