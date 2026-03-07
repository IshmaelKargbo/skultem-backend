package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.UserSession;
import com.moriba.skultem.domain.repository.UserSessionRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.UserSessionJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.UserSessionMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserSessionAdapter implements UserSessionRepository {

    private final UserSessionJpaRepository repo;

    @Override
    public void save(UserSession domain) {
        var entity = UserSessionMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<UserSession> findById(String id) {
        return repo.findByIdWithUser(id).map(UserSessionMapper::toDomain);
    }

    @Override
    public List<UserSession> findAllByUserAndSchoolIdAndActive(String userId, String schoolId, boolean active) {
        return repo.findAllByUser_IdAndSchoolIdAndActive(userId, schoolId, active).stream()
                .map(UserSessionMapper::toDomain).toList();
    }

}
