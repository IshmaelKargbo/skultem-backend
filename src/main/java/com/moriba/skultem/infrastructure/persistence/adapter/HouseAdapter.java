package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.House;
import com.moriba.skultem.domain.repository.HouseRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.HouseJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.HouseMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class HouseAdapter implements HouseRepository {
    private final HouseJpaRepository repo;

    @Override
    public void save(House domain) {
        var entity = HouseMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public void delete(House domain) {
        repo.deleteById(domain.getId());
    }

    @Override
    public boolean existByNameAndSchoolId(String name, String schoolId) {
        return repo.existsByNameIgnoreCaseAndSchoolId(name, schoolId);
    }

    @Override
    public Optional<House> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(HouseMapper::toDomain);
    }

    @Override
    public Page<House> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(HouseMapper::toDomain);
    }

    @Override
    public Page<House> search(String value, String schoolId, Pageable pageable) {
        return repo.search(schoolId, value, pageable).map(HouseMapper::toDomain);
    }
}
