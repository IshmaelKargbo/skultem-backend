package com.moriba.skultem.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Activity;
import com.moriba.skultem.domain.repository.ActivityRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ActivityJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ActivityMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ActivityAdapter implements ActivityRepository {

    private final ActivityJpaRepository repo;

    @Override
    public void save(Activity domain) {
        repo.save(ActivityMapper.toEntity(domain));
    }

    @Override
    public Page<Activity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(ActivityMapper::toDomain);
    }
}
