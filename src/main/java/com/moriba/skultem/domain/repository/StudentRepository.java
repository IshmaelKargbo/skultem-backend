package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Student;

public interface StudentRepository {
    void save(Student domain);

    Optional<Student> findByIdAndSchoolId(String id, String schoolId);

    boolean existsByAdmissionNumberAndSchoolId(String admissionNumber, String schoolId);

    Page<Student> findBySchoolId(String schoolId, Pageable pageable);

    long countAll();
}
