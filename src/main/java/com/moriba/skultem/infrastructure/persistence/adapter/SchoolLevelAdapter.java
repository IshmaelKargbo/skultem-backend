package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.SchoolLevel;
import com.moriba.skultem.domain.repository.SchoolLevelRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.SchoolLevelJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.SchoolLevelMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SchoolLevelAdapter implements SchoolLevelRepository {
    private final SchoolLevelJpaRepository repo;

    @Override
    public void save(SchoolLevel domain) {
        repo.save(SchoolLevelMapper.toEntity(domain));
    }

    // Youngest to oldest (Level declaration order), not insertion order.
    @Override
    public List<SchoolLevel> findBySchoolId(String schoolId) {
        return repo.findAllBySchoolId(schoolId).stream()
                .map(SchoolLevelMapper::toDomain)
                .sorted(Comparator.comparing(SchoolLevel::getLevel))
                .toList();
    }

    @Override
    public void delete(SchoolLevel domain) {
        repo.deleteById(domain.getId());
    }
}
