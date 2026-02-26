package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.SubjectGroup;
import com.moriba.skultem.domain.repository.SubjectGroupRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.SubjectGroupJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.SubjectGroupMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SubjectGroupAdapter implements SubjectGroupRepository {
    private final SubjectGroupJpaRepository repo;

    @Override
    public void save(SubjectGroup domain) {
        var entity = SubjectGroupMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Page<SubjectGroup> findBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(SubjectGroupMapper::toDomain);
    }

    @Override
    public Optional<SubjectGroup> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(SubjectGroupMapper::toDomain);
    }

    @Override
    public List<SubjectGroup> findAllByIdsAndSchoolId(Set<String> ids, String schoolId) {
        if (ids == null || ids.isEmpty() || schoolId == null) {
            return List.of();
        }

        return repo.findAllByIdInAndSchoolId(ids, schoolId).stream()
                .map(SubjectGroupMapper::toDomain)
                .toList();
    }

    @Override
    public List<SubjectGroup> findAllByIdInAndStreamAndSchoolId(Set<String> ids, String streamId, String schoolId) {
        if (ids == null || ids.isEmpty() || streamId == null || schoolId == null) {
            return List.of();
        }

        return repo.findAllByIdInAndStream_IdAndSchoolId(ids, streamId, schoolId).stream()
                .map(SubjectGroupMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<SubjectGroup> findByIdAndStreamAndSchoolId(String id, String streamId, String schoolId) {
        return repo.findByIdAndStream_IdAndSchoolId(id, streamId, schoolId).map(SubjectGroupMapper::toDomain);
    }

    @Override
    public Optional<SubjectGroup> findByIdAndClassSchoolId(String id, String classId, String schoolId) {
        return repo.findByIdAndClazz_IdAndSchoolId(id, classId, schoolId).map(SubjectGroupMapper::toDomain);
    }

    @Override
    public Page<SubjectGroup> findByClassAndSchool(String classId, String schoolId, Pageable pageable) {
        return repo.findAllByClazz_IdAndSchoolIdOrderByClazz_LevelOrderAsc(classId, schoolId, pageable).map(SubjectGroupMapper::toDomain);
    }

    @Override
    public Page<SubjectGroup> findByStreamIdAndSchoolId(String streamId, String schoolId, Pageable pageable) {
        return repo.findAllByStream_IdAndSchoolIdOrderByClazz_LevelOrderAsc(streamId, schoolId, pageable).map(SubjectGroupMapper::toDomain);
    }
}
