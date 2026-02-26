package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Stream;
import com.moriba.skultem.domain.repository.StreamRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.StreamJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.StreamMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StreamAdapter implements StreamRepository {
    private final StreamJpaRepository repo;

    @Override
    public void save(Stream domain) {
        var entity = StreamMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public boolean existsByNameAndSchool(String name, String schoolId) {
        return repo.existsByNameIgnoreCaseAndSchoolId(name, schoolId);
    }

    @Override
    public Page<Stream> findBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(StreamMapper::toDomain);
    }

    @Override
    public Optional<Stream> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(StreamMapper::toDomain);
    }
}
