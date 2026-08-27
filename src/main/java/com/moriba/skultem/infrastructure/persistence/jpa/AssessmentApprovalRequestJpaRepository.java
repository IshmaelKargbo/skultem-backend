package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.domain.model.AssessmentApprovalRequest;
import com.moriba.skultem.infrastructure.persistence.entity.AssessmentApprovalRequestEntity;

public interface AssessmentApprovalRequestJpaRepository
        extends JpaRepository<AssessmentApprovalRequestEntity, String> {
    @Query(value = """
                SELECT aar
                FROM AssessmentApprovalRequestEntity aar
                JOIN aar.teacherSubject ts
                JOIN ts.session s
                JOIN s.academicYear ac
                JOIN ClassMasterEntity cm ON cm.session = s
                WHERE cm.teacher.id = :teacherId
                  AND ac.id = :academicYearId
                  AND cm.endedAt IS NULL
                  AND (:status IS NULL OR aar.status = :status)
            """, countQuery = """
                SELECT COUNT(aar)
                FROM AssessmentApprovalRequestEntity aar
                JOIN aar.teacherSubject ts
                JOIN ts.session s
                JOIN s.academicYear ac
                JOIN ClassMasterEntity cm ON cm.session = s
                WHERE cm.teacher.id = :teacherId
                  AND ac.id = :academicYearId
                  AND cm.endedAt IS NULL
                  AND (:status IS NULL OR aar.status = :status)
            """)
    Page<AssessmentApprovalRequestEntity> findAllForClassMasterByTeacherId(
            @Param("teacherId") String teacherId,
            @Param("academicYearId") String academicYearId,
            @Param("status") AssessmentApprovalRequest.Status status,
            Pageable pageable);

    @Query("""
                SELECT COUNT(aar)
                FROM AssessmentApprovalRequestEntity aar
                JOIN aar.teacherSubject ts
                JOIN ts.session s
                JOIN s.academicYear ac
                JOIN ClassMasterEntity cm ON cm.session = s
                WHERE cm.teacher.id = :teacherId
                  AND ac.id = :academicYearId
                  AND cm.endedAt IS NULL
                  AND aar.status = :status
            """)
    long countForClassMasterByTeacherIdAndStatus(
            @Param("teacherId") String teacherId,
            @Param("academicYearId") String academicYearId,
            @Param("status") AssessmentApprovalRequest.Status status);

    Optional<AssessmentApprovalRequestEntity> findByIdAndSchoolId(String id, String schoolId);

    boolean existsByCycle_IdAndTeacherSubject_Id(String cycleId, String subjectId);

    Optional<AssessmentApprovalRequestEntity> findByCycle_IdAndTeacherSubject_Id(String cycleId, String subjectId);
}
