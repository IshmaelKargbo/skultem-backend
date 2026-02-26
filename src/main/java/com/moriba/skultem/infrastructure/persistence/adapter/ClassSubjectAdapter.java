package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ClassSubject;
import com.moriba.skultem.domain.repository.ClassSubjectRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ClassSubjectJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ClassSubjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClassSubjectAdapter implements ClassSubjectRepository {
    private final ClassSubjectJpaRepository repo;

    @Override
    public void save(ClassSubject domain) {
        var entity = ClassSubjectMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<ClassSubject> findById(String id) {
        return repo.findById(id).map(ClassSubjectMapper::toDomain);
    }

    @Override
    public Optional<ClassSubject> findByClassIdAndSubjectId(String classId, String subjectId, String schoolId) {
        return repo.findByClazz_IdAndSubject_IdAndSchoolId(classId, subjectId, schoolId)
                .map(ClassSubjectMapper::toDomain);
    }

    @Override
    public boolean existsByClassAndSubject(String classId, String subjectId) {
        return repo.existsByClazz_IdAndSubject_Id(classId, subjectId);
    }

    @Override
    public Page<ClassSubject> findByClass(String classId, Pageable pageable) {
        return repo.findAllByClazz_Id(classId, pageable).map(ClassSubjectMapper::toDomain);
    }

    @Override
    public Page<ClassSubject> findBySchool(String school, Pageable pageable) {
        return repo.findAllBySchoolId(school, pageable).map(ClassSubjectMapper::toDomain);
    }

    @Override
    public void delete(ClassSubject domain) {
        var entity = ClassSubjectMapper.toEntity(domain);
        repo.delete(entity);
    }

    @Override
    public boolean existsByClassIdAndSubjectIdAndSchoolId(String classId, String subjectId, String schoolId) {
        return repo.existsByClazz_IdAndSubject_IdAndSchoolId(classId, subjectId, schoolId);
    }

    @Override
    public Optional<ClassSubject> findByClassIdAndSubjectIdAndSchoolId(String classId, String subjectId,
            String schoolId) {
        return repo.findByClazz_IdAndSubject_IdAndSchoolId(classId, subjectId, schoolId)
                .map(ClassSubjectMapper::toDomain);
    }

    @Override
    public Page<ClassSubject> findAllByClassIdAndSchoolId(String classId, String schoolId, Pageable pagable) {
        return repo.findAllByClazz_IdAndSchoolId(classId, schoolId, pagable)
                .map(ClassSubjectMapper::toDomain);
    }
}
