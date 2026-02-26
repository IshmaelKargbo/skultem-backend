package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Subject;
import com.moriba.skultem.domain.repository.SubjectRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.SubjectJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.SubjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SubjectAdapter implements SubjectRepository {
    private final SubjectJpaRepository repo;

    @Override
    public void save(Subject domain) {
        var entity = SubjectMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Subject> findById(String id) {
        return repo.findById(id).map(SubjectMapper::toDomain);
    }

    @Override
    public boolean existsByCodeAndSchool(String code, String school) {
        return repo.existsByCodeIgnoreCaseAndSchoolId(code, school);
    }

    @Override
    public Page<Subject> findBySchool(String school, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(school, pageable).map(SubjectMapper::toDomain);
    }

    @Override
    public void delete(Subject domain) {
        var entity = SubjectMapper.toEntity(domain);
        repo.delete(entity);
    }

    @Override
    public Optional<Subject> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(SubjectMapper::toDomain);
    }

    @Override
    public List<Subject> findAllByIdInAndSchoolId(Set<String> ids, String schoolId) {
        if (ids == null || ids.isEmpty() || schoolId == null) {
            return List.of();
        }
        return repo.findAllByIdInAndSchoolId(ids, schoolId).stream()
                .map(SubjectMapper::toDomain)
                .toList();
    }
}
