package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.model.Student.Status;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;

public interface StudentJpaRepository extends JpaRepository<StudentEntity, String> {

    boolean existsByAdmissionNumberAndSchoolId(String admissionNumber, String schoolId);

    boolean existsByGivenNamesIgnoreCaseAndFamilyNameIgnoreCaseAndDateOfBirthAndSchoolId(String givenNames,
            String familyName, LocalDate dateOfBirth, String schoolId);

    boolean existsByAdmissionNumberAndSchoolIdAndIdNot(String admissionNumber, String schoolId, String id);

    @Query("""
        SELECT s
        FROM StudentEntity s
        WHERE s.schoolId = :schoolId
          AND s.status = :status
          AND EXISTS (
                SELECT 1 FROM EnrollmentEntity e
                WHERE e.student = s
                  AND e.schoolId = :schoolId
                  AND e.academicYear.id = :academicYearId
                  AND (:classId = '' OR e.clazz.id = :classId)
                  AND e.clazz.level IN :levels
          )
          AND (:gender = '' OR CAST(s.gender AS string) = :gender)
          AND (
                :search IS NULL
             OR :search = ''
             OR LOWER(s.givenNames) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(s.familyName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(CAST(s.admissionNumber AS string)) LIKE LOWER(CONCAT('%', :search, '%'))
          )
        """)
    Page<StudentEntity> search(
            @Param("schoolId") String schoolId,
            @Param("search") String search,
            @Param("academicYearId") String academicYearId,
            @Param("status") Status status,
            @Param("classId") String classId,
            @Param("gender") String gender,
            @Param("levels") Collection<Level> levels,
            Pageable pageable
    );

    Page<StudentEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

    Page<StudentEntity> findAllBySchoolIdAndParent_IdOrderByCreatedAtDesc(String schoolId, String parentId,
            Pageable pageable);

    Optional<StudentEntity> findByIdAndSchoolId(String id, String schoolId);
}
