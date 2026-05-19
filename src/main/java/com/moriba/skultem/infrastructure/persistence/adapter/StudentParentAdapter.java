package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.StudentParent;
import com.moriba.skultem.domain.repository.StudentParentRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.StudentParentJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.StudentParentMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StudentParentAdapter implements StudentParentRepository {

    private final StudentParentJpaRepository repo;

    @Override
    public void save(StudentParent domain) {
        var entity = StudentParentMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public void delete(StudentParent domain) {
        var entity = StudentParentMapper.toEntity(domain);
        repo.delete(entity);
    }

    @Override
    public boolean existByStudentIdAndParentIdAndSchoolId(String studentId, String parentId, String schoolId) {
        return repo.existsByStudentIdAndParentIdAndSchoolId(studentId, parentId, schoolId);
    }

    @Override
    public Optional<StudentParent> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(StudentParentMapper::toDomain);
    }

    @Override
    public Page<StudentParent> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(StudentParentMapper::toDomain);
    }

    @Override
    public Optional<StudentParent> findByStudentAndSchool(String studentId, String schoolId) {
        return repo.findByStudentIdAndSchoolId(studentId, schoolId).map(StudentParentMapper::toDomain);
    }

    @Override
    public List<StudentParent> findAllByStudentAndSchool(String studentId, String schoolId) {
        return repo.findAllByStudentIdAndSchoolId(studentId, schoolId).stream()
                .map(StudentParentMapper::toDomain)
                .toList();
    }

}
