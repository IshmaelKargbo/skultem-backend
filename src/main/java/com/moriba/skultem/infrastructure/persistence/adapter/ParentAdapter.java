package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Parent;
import com.moriba.skultem.domain.repository.ParentRepository;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.jpa.ParentJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ParentMapper;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ParentAdapter implements ParentRepository {

    private final ParentJpaRepository repo;

    @Override
    public void save(Parent domain) {
        var entity = ParentMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Parent> findById(String id) {
        return repo.findById(id).map(ParentMapper::toDomain);
    }

    @Override
    public long countAll() {
        return repo.count();
    }

    @Override
    public Page<Parent> findBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(ParentMapper::toDomain);
    }

    @Override
    public boolean existsByPhoneAndSchool(String phone, String schoolId) {
        return repo.existsByPhoneAndSchoolId(phone, schoolId);
    }

    @Override
    public Optional<Parent> findByIdAndSchoolId(String id, String school) {
        return repo.findByIdAndSchoolId(id, school).map(ParentMapper::toDomain);
    }

    @Override
    public Optional<Parent> findByUserIdAndSchoolId(String userId, String schoolId) {
        return repo.findByUser_IdAndSchoolId(userId, schoolId).map(ParentMapper::toDomain);
    }

    @Override
    public Page<Parent> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
        return repo.runReport(schoolId, filters, pageable)
                .map(ParentMapper::toDomain);
    }
}
