package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Subject;

public interface SubjectRepository {
    void save(Subject domain);

    Optional<Subject> findById(String id);

    Optional<Subject> findByIdAndSchoolId(String id, String schoolId);

    List<Subject> findAllByIdInAndSchoolId(Set<String> ids, String schoolId);

    boolean existsByCodeAndSchool(String code, String school);

    Page<Subject> findBySchool(String school, Pageable pageable);

    void delete(Subject domain);
}
