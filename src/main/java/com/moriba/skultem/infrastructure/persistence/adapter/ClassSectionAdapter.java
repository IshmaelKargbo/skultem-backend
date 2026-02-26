package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ClassSection;
import com.moriba.skultem.domain.repository.ClassSectionRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ClassSectionJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ClassSectionMapper;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClassSectionAdapter implements ClassSectionRepository {

    private final ClassSectionJpaRepository repo;

    @Override
    public void save(ClassSection domain) {
        var entity = ClassSectionMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public void delete(ClassSection domain) {
        var entity = ClassSectionMapper.toEntity(domain);
        repo.delete(entity);
    }

    @Override
    public Optional<ClassSection> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(ClassSectionMapper::toDomain);
    }

    @Override
    public boolean existsBySchoolAndClassId(String schoolId, String classId) {
        return repo.existsByClazz_IdAndSchoolId(classId, schoolId);
    }

    @Override
    public List<ClassSection> findByClassIdAndSchoolId(String classId, String schoolId) {
        return repo.findAllByClazz_IdAndSchoolId(classId, schoolId).stream().map(ClassSectionMapper::toDomain).toList();
    }

    @Override
    public Optional<ClassSection> findByIdAndClassIdAndSchoolId(String id, String classId, String schoolId) {
        return repo.findByIdAndClazz_IdAndSchoolId(id, classId, schoolId)
                .map(ClassSectionMapper::toDomain);
    }

    @Override
    public boolean existsByClassIdAndSchoolIdAndSectionId(String classId, String schoolId, String sectionId) {
        return repo.existsByClazz_IdAndSchoolIdAndSection_Id(classId, schoolId, sectionId);
    }
}
