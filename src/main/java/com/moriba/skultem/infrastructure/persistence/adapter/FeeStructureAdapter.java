package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.FeeStructure;
import com.moriba.skultem.domain.repository.FeeStructureRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.FeeStructureJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.FeeStructureMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FeeStructureAdapter implements FeeStructureRepository {
    private final FeeStructureJpaRepository repo;

    @Override
    public void save(FeeStructure domain) {
        var entity = FeeStructureMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<FeeStructure> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(FeeStructureMapper::toDomain);
    }

    @Override
    public boolean existsBySchoolAndAcademicYearAndTermAndClassAndCategory(String schoolId, String academicYearId,
            String termId, String classId, String categorId) {
        return repo.existsByAcademicYear_IdAndClazz_IdAndTerm_IdAndCategory_IdAndSchoolId(academicYearId, classId,
                termId, categorId, schoolId);
    }

    @Override
    public Page<FeeStructure> findBySchoolAndClass(String schoolId, String classId, Pageable pageable) {
        return repo.findAllByClazz_IdAndSchoolIdOrderByCreatedAtDesc(classId, schoolId, pageable)
                .map(FeeStructureMapper::toDomain);
    }

    @Override
    public Page<FeeStructure> findBySchoolAndAcademic(String schoolId, String academicYearId, Pageable pageable) {
        return repo.findAllByAcademicYear_IdAndSchoolIdOrderByCreatedAtDesc(academicYearId, schoolId, pageable)
                .map(FeeStructureMapper::toDomain);
    }

    @Override
    public Page<FeeStructure> findBySchoolAndAcademicAndClass(String schoolId, String academicYearId, String classId,
            Pageable pageable) {
        return repo
                .findAllByAcademicYear_IdAndClazz_IdAndSchoolIdOrderByCreatedAtDesc(academicYearId, classId, schoolId,
                        pageable)
                .map(FeeStructureMapper::toDomain);
    }

    @Override
    public Page<FeeStructure> findAllBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(FeeStructureMapper::toDomain);
    }

    @Override
    public List<FeeStructure> findApplicableFees(String schoolId, String academicYearId, String classId) {
        return repo.findApplicableFees(schoolId, academicYearId, classId).stream().map(FeeStructureMapper::toDomain)
                .toList();
    }

}
