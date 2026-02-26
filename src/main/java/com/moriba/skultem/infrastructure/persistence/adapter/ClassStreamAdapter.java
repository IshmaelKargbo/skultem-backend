package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ClassStream;
import com.moriba.skultem.domain.repository.ClassStreamRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ClassStreamJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ClassStreamMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClassStreamAdapter implements ClassStreamRepository {
    private final ClassStreamJpaRepository repo;

    @Override
    public void save(ClassStream domain) {
        var entity = ClassStreamMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<ClassStream> findById(String id) {
        return repo.findById(id).map(ClassStreamMapper::toDomain);
    }

    @Override
    public Optional<ClassStream> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(ClassStreamMapper::toDomain);
    }

    @Override
    public long countAll() {
        return repo.count();
    }

    @Override
    public boolean existsByClassIdAndSchoolIdAndStreamId(String classId, String schoolId, String streamId) {
        return repo.existsByClazz_IdAndSchoolIdAndStream_Id(classId, schoolId, streamId);
    }

    @Override
    public Optional<ClassStream> findByClassIdAndSchoolId(String classId, String schoolId) {
        return repo.findByClazz_IdAndSchoolId(classId, schoolId).map(ClassStreamMapper::toDomain);
    }

    @Override
    public Optional<ClassStream> findByClassIdAndSchoolIdAndStreamId(String classId, String schoolId, String streamId) {
        return repo.findByClazz_IdAndStream_IdAndSchoolId(classId, streamId, schoolId).map(ClassStreamMapper::toDomain);
    }

    @Override
    public List<ClassStream> findAllByClassIdAndSchoolId(String classId, String schoolId) {
        return repo.findAllByClazz_IdAndSchoolId(classId, schoolId).stream().map(ClassStreamMapper::toDomain).toList();
    }

}
