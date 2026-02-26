package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.User;
import com.moriba.skultem.domain.repository.UserRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.UserJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserAdapter implements UserRepository {
    private final UserJpaRepository repo;

    @Override
    public void save(User domain) {
        var entity = UserMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<User> findById(String id) {
        return repo.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repo.existsByEmailIgnoreCase(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repo.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public Page<User> findBySchool(String school, Pageable pageable) {
        return repo.findAllBySchoolId(school, pageable).map(UserMapper::toDomain);
    }

    @Override
    public long countAll() {
        return repo.count();
    }

    @Override
    public void delete(User domain) {
        var entity = UserMapper.toEntity(domain);
        repo.delete(entity);
    }
}
