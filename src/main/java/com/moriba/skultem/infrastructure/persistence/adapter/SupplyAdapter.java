package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Supply;
import com.moriba.skultem.domain.repository.SupplyRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.SupplyJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.SupplyMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SupplyAdapter implements SupplyRepository {
    private final SupplyJpaRepository repo;

    @Override
    public void save(Supply domain) {
        var entity = SupplyMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public boolean existsByStudentIdAndMaterialIdAndSchoolId(String studentId, String materialId, String schoolId) {
        return repo.existsByStudentIdAndMaterialIdAndSchoolId(studentId, materialId, schoolId);
    }

    @Override
    public Optional<Supply> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(SupplyMapper::toDomain);
    }

    @Override
    public Page<Supply> findBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(SupplyMapper::toDomain);
    }

    @Override
    public Page<Supply> findByStudentAndSchool(String studentId, String schoolId, Pageable pageable) {
        return repo.findAllByStudentIdAndSchoolIdOrderByCreatedAtDesc(studentId, schoolId, pageable)
                .map(SupplyMapper::toDomain);
    }

}
