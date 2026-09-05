package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.SalaryStructure;
import com.moriba.skultem.domain.repository.SalaryStructureRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.SalaryStructureJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.SalaryStructureMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SalaryStructureAdapter implements SalaryStructureRepository {
    private final SalaryStructureJpaRepository repo;

    @Override
    public void save(SalaryStructure domain) {
        repo.save(SalaryStructureMapper.toEntity(domain));
    }

    @Override
    public Optional<SalaryStructure> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(SalaryStructureMapper::toDomain);
    }

    @Override
    public Optional<SalaryStructure> findByTeacherIdAndSchoolId(String teacherId, String schoolId) {
        return repo.findByTeacher_IdAndSchoolId(teacherId, schoolId).map(SalaryStructureMapper::toDomain);
    }

    @Override
    public boolean existsByTeacherIdAndSchoolId(String teacherId, String schoolId) {
        return repo.existsByTeacher_IdAndSchoolId(teacherId, schoolId);
    }

    @Override
    public Page<SalaryStructure> search(String value, String schoolId, Pageable pageable) {
        return repo.search(schoolId, value, pageable).map(SalaryStructureMapper::toDomain);
    }

    @Override
    public List<SalaryStructure> findAllBySchoolId(String schoolId) {
        return repo.findAllBySchoolId(schoolId).stream().map(SalaryStructureMapper::toDomain).toList();
    }

    @Override
    public Page<SalaryStructure> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(SalaryStructureMapper::toDomain);
    }
}
