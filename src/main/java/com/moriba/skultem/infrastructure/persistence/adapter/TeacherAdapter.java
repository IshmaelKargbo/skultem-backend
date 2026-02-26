package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Teacher;
import com.moriba.skultem.domain.repository.TeacherRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.TeacherJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TeacherAdapter implements TeacherRepository {

    private final TeacherJpaRepository repo;

    @Override
    public void save(Teacher domain) {
        var entity = TeacherMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Teacher> findById(String id) {
        return repo.findById(id).map(TeacherMapper::toDomain);
    }

    @Override
    public long countAll() {
        return repo.count();
    }

    @Override
    public boolean existsByStaffIdAndSchool(String staffId, String schoolId) {
        return repo.existsByStaffIdAndSchoolId(staffId, schoolId);
    }

    @Override
    public Page<Teacher> findBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(TeacherMapper::toDomain);
    }

    @Override
    public boolean existsByPhoneAndSchool(String phone, String schoolId) {
        return repo.existsByPhoneAndSchoolId(phone, schoolId);
    }

    @Override
    public Optional<Teacher> findByUserId(String userId) {
        return repo.findByUserId(userId).map(TeacherMapper::toDomain);
    }

    @Override
    public Optional<Teacher> findByIdAndSchoolId(String id, String school) {
        return repo.findByIdAndSchoolId(id, school).map(TeacherMapper::toDomain);
    }
}
