package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.EnrollmentSubject;

public interface EnrollmentSubjectRepository {
    void save(EnrollmentSubject domain);

    Optional<EnrollmentSubject> findById(String id);

    boolean existsByEnrollmentIdAndSubjectIdAndSchoolId(String enrollmentId, String subjectId, String schoolId);

    List<EnrollmentSubject> findAllByEnrollmentIdAndSchoolId(String enrollmentId, String schoolId);

    Page<EnrollmentSubject> findBySchoolId(String schoolId, Pageable pageable);

    long countByEnrollmentIdAndSchoolId(String enrollmentId, String schoolId);

    long countAll();

    void delete(EnrollmentSubject domain);

    void deleteByEnrollmentIdAndSubjectIdAndSchoolId(String enrollmentId, String subjectId, String schoolId);
}
