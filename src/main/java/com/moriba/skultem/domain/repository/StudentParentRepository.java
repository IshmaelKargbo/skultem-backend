package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.StudentParent;

public interface StudentParentRepository {
    void save(StudentParent domain);

    void delete(StudentParent domain);

    boolean existByStudentIdAndParentIdAndSchoolId(String studentId, String parentId, String schoolId);

    Optional<StudentParent> findByIdAndSchool(String id, String schoolId);

    Optional<StudentParent> findByStudentAndSchool(String studentId, String schoolId);

    List<StudentParent> findAllByStudentAndSchool(String studentId, String schoolId);

    Page<StudentParent> findAllBySchoolId(String schoolId, Pageable pageable);

}
