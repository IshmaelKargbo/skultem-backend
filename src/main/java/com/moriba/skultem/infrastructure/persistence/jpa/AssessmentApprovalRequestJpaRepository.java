package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.AssessmentApprovalRequestEntity;

@Repository
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
            """)
    Page<AssessmentApprovalRequestEntity> findAllForClassMasterByTeacherId(
            @Param("teacherId") String teacherId,
            @Param("academicYearId") String academicYearId,
            Pageable pageable);

    Optional<AssessmentApprovalRequestEntity> findByIdAndSchoolId(String id, String schoolId);

    boolean existsByCycle_IdAndTeacherSubject_Id(String cycleId, String subjectId);

    Optional<AssessmentApprovalRequestEntity> findByCycle_IdAndTeacherSubject_Id(String cycleId, String subjectId);
}
