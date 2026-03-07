package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.SchoolUser;
import com.moriba.skultem.domain.repository.SchoolUserRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.SchoolUserJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.SchoolUserMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SchoolUserAdapter implements SchoolUserRepository {
    private final SchoolUserJpaRepository repo;

    @Override
    public void save(SchoolUser domain) {
        var entity = SchoolUserMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<SchoolUser> findBySchoolAndUser(String schoolId, String userId) {
        return repo.findBySchoolIdAndUserIdWithUser(schoolId, userId).map(SchoolUserMapper::toDomain);
    }

    @Override
    public boolean existsBySchoolAndUser(String schoolId, String userId) {
        return repo.existsBySchoolIdAndUser_Id(schoolId, userId);
    }

    @Override
    public List<SchoolUser> findBySchool(String schoolId) {
        return repo.findAllBySchoolId(schoolId).stream().map(SchoolUserMapper::toDomain).toList();
    }

    @Override
    public Optional<SchoolUser> findOneByUser(String userId) {
        return repo.findOneByUserIdWithUser(userId).map(SchoolUserMapper::toDomain);
    }

    @Override
    public List<SchoolUser> findAllByUser_IdAndSchoolId(String userId, String schoolId) {
        return repo.findAllByUserIdWithUser(userId, schoolId).stream().map(SchoolUserMapper::toDomain).toList();
    }
}
