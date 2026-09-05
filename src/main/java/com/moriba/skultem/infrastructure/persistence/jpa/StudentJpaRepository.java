package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.model.Student.Status;
import com.moriba.skultem.infrastructure.persistence.entity.StudentEntity;

public interface StudentJpaRepository extends JpaRepository<StudentEntity, String> {
        boolean existsByAdmissionNumberAndSchoolId(String admissionNumber, String schoolId);

        // Scoped to students who haven't been deleted and have some enrollment for the given academic
        // year, so a student with no relationship to that year at all doesn't linger in this listing
        // (and every picker built on it: behaviour, discounts, supplies, payments, reports, ...).
        // Deliberately *not* filtered to Student.status = ACTIVE - a student who's since graduated or
        // was promoted/repeated (see ApprovePromotionRequestUseCase) is still part of that year's
        // history and can still owe fees for it, so only an actual (soft) delete excludes them.
        // No ORDER BY here - the caller's Pageable carries the Sort (see StudentService.search /
        // ListStudentBySchoolUseCase), and a fixed order here would either dominate or conflict
        // with it. classId is always a real (possibly empty) string, never null - see the note on
        // AssessmentApprovalRequestJpaRepository for why a null bound into a comparison like this
        // is asking for trouble.
        @Query("""
                            SELECT s
                            FROM StudentEntity s
                            WHERE s.schoolId = :schoolId
                              AND s.status <> :excludedStatus
                              AND EXISTS (
                                    SELECT 1 FROM EnrollmentEntity e
                                    WHERE e.student = s
                                      AND e.schoolId = :schoolId
                                      AND e.academicYear.id = :academicYearId
                                      AND (:classId = '' OR e.clazz.id = :classId)
                              )
                              AND (
                                    :search IS NULL
                                 OR :search = ''
                                 OR LOWER(s.givenNames) LIKE LOWER(CONCAT('%', :search, '%'))
                                 OR LOWER(s.familyName) LIKE LOWER(CONCAT('%', :search, '%'))
                                 OR LOWER(CAST(s.admissionNumber AS string)) LIKE LOWER(CONCAT('%', :search, '%'))
                              )
                        """)
        Page<StudentEntity> search(@Param("schoolId") String schoolId, @Param("search") String search,
                        @Param("academicYearId") String academicYearId, @Param("excludedStatus") Status excludedStatus,
                        @Param("classId") String classId, Pageable pageable);

        Page<StudentEntity> findAllBySchoolIdOrderByCreatedAtDesc(String schoolId, Pageable pageable);

        Page<StudentEntity> findAllBySchoolIdAndParent_IdOrderByCreatedAtDesc(String schoolId, String parentId,
                        Pageable pageable);

        Optional<StudentEntity> findByIdAndSchoolId(String id, String schoolId);
}
