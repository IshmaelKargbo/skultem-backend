package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Student;
import com.moriba.skultem.domain.repository.StudentRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.StudentJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.StudentMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StudentAdapter implements StudentRepository {
    private final StudentJpaRepository repo;

    @Override
    public void save(Student domain) {
        var entity = StudentMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public boolean existsByAdmissionNumberAndSchoolId(String admissionNumber, String schoolId) {
        return repo.existsByAdmissionNumberAndSchoolId(admissionNumber, schoolId);
    }

    @Override
    public Page<Student> findBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(StudentMapper::toDomain);
    }

    @Override
    public long countAll() {
        return repo.count();
    }

    @Override
    public Optional<Student> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(StudentMapper::toDomain);
    }

    @Override
    public Page<Student> findByParentAndSchoolId(String parentId, String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdAndParent_IdOrderByCreatedAtDesc(schoolId, parentId, pageable).map(StudentMapper::toDomain);
    }
}
