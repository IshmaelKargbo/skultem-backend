package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Teacher;

public interface TeacherRepository {
    void save(Teacher domain);

    Optional<Teacher> findById(String id);

    Optional<Teacher> findByIdAndSchoolId(String id, String school);

    Optional<Teacher> findByUserId(String userId);

    boolean existsByStaffIdAndSchool(String staffId, String schoolId);

    boolean existsByPhoneAndSchool(String phone, String schoolId);

    Page<Teacher> findBySchool(String schoolId, Pageable pageable);

    long countAll();
}
