package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.repository.ClassSessionRepository;
import com.moriba.skultem.infrastructure.persistence.entity.ClassSessionEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.ClassSessionJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ClassSessionMapper;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClassSessionAdapter implements ClassSessionRepository {
    private final ClassSessionJpaRepository repo;

    @Override
    public void save(ClassSession domain) {
        var entity = ClassSessionMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public boolean existsByClassIdAndAcademicYearIdAndStreamIdAndSchoolId(String classId, String academicYearId,
            String streamId, String schoolId) {
        return repo.existsByClazz_IdAndAcademicYear_IdAndStream_IdAndSchoolId(classId, academicYearId, streamId,
                schoolId);
    }

    @Override
    public Page<ClassSession> findBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(ClassSessionMapper::toDomain);
    }

    @Override
    public long countBySchoolId(String schoolId) {
        return repo.countBySchoolId(schoolId);
    }

    @Override
    public long countAll() {
        return repo.count();
    }

    @Override
    public void saveAll(List<ClassSession> domains) {
        List<ClassSessionEntity> entities = domains.stream()
                .map(ClassSessionMapper::toEntity)
                .toList();
        repo.saveAll(entities);
    }

    @Override
    public boolean existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(String classId,
            String academicYearId, String sectionId, String streamId, String schoolId) {
        return repo.existsByClazz_IdAndAcademicYear_IdAndSection_IdAndStream_IdAndSchoolId(classId, academicYearId,
                sectionId, streamId, schoolId);
    }

    @Override
    public boolean existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIsNullAndSchoolId(String classId,
            String academicYearId, String sectionId, String schoolId) {
        return repo.existsByClazz_IdAndAcademicYear_IdAndSection_IdAndStreamIsNullAndSchoolId(classId, academicYearId,
                sectionId, schoolId);
    }

    @Override
    public Optional<ClassSession> findByClassIdAndAcademicYearIdAndSchoolId(String classId, String academicYearId,
            String schoolId) {
        return repo.findByClazz_IdAndAcademicYear_IdAndSchoolId(classId, academicYearId, schoolId)
                .map(ClassSessionMapper::toDomain);
    }

    @Override
    public Optional<ClassSession> findByClassIdAndAcademicYearIdAndSectionIdAndSchoolId(String classId,
            String academicYearId, String sectionId, String schoolId) {
        return repo
                .findByClazz_IdAndAcademicYear_IdAndSection_IdAndSchoolId(classId, academicYearId, sectionId, schoolId)
                .map(ClassSessionMapper::toDomain);
    }

    @Override
    public Optional<ClassSession> findByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(String classId,
            String academicYearId, String sectionId, String streamId, String schoolId) {
        return repo
                .findByClazz_IdAndAcademicYear_IdAndSection_IdAndStream_IdAndSchoolId(classId, academicYearId,
                        sectionId,
                        streamId, schoolId)
                .map(ClassSessionMapper::toDomain);
    }

    @Override
    public Optional<ClassSession> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(ClassSessionMapper::toDomain);
    }

    @Override
    public Page<ClassSession> findBySchoolIdAndAcademicYearId(String schoolId, String academicYearId,
            Pageable pageable) {
        return repo.findBySchoolIdAndAcademicYear_Id(schoolId, academicYearId, pageable)
                .map(ClassSessionMapper::toDomain);
    }

    @Override
    public List<ClassSession> findAllByClassIdAndAcademicYearIdAndSchoolId(String classId, String academicYearId,
            String schoolId) {
        return repo.findAllByClazz_IdAndAcademicYear_IdAndSchoolId(classId, academicYearId, schoolId)
                .stream()
                .map(ClassSessionMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ClassSession> findByAcademicYearAndClassAndSchoolId(String academic, String classId,
            String schoolId) {
        return repo.findByAcademicYear_IdAndClazz_IdAndSchoolId(academic, classId, schoolId)
                .map(ClassSessionMapper::toDomain);
    }
}
