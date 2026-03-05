package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.EnrollmentSubjectEntity;

@Repository
public interface EnrollmentSubjectJpaRepository
                extends JpaRepository<EnrollmentSubjectEntity, String> {

        boolean existsByEnrollment_IdAndSubject_IdAndSchoolId(String enrollmentId, String subjectId, String schoolId);

        Optional<EnrollmentSubjectEntity> findByIdAndSchoolId(String id, String schoolId);

        long countByEnrollment_IdAndSchoolId(String enrollmentId, String schoolId);

        Page<EnrollmentSubjectEntity> findAllBySchoolId(String schoolId, Pageable pageable);
        
        List<EnrollmentSubjectEntity> findAllByEnrollment_IdAndSchoolId(String enrollmentId, String schoolId);

        void deleteByEnrollment_IdAndSubject_IdAndSchoolId(String enrollmentId, String subjectId, String schoolId);
}
