package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.AssessmentApprovalRequestEntity;

@Repository
public interface AssessmentApprovalRequestJpaRepository
        extends JpaRepository<AssessmentApprovalRequestEntity, String> {

    @Query("""
        SELECT aar
        FROM AssessmentApprovalRequestEntity aar
        JOIN aar.teacherSubject ts
        JOIN ts.session s
        JOIN s.academicYear ac
        JOIN ClassMasterEntity cm ON cm.session = s
        WHERE cm.teacher.id = :teacherId
          AND ac.id = :academicYearId
          AND cm.endedAt IS NULL
    """)
    List<AssessmentApprovalRequestEntity> findAllForClassMasterByTeacherId(
            @Param("teacherId") String teacherId,
            @Param("academicYearId") String academicYearId
    );

    Optional<AssessmentApprovalRequestEntity> findByIdAndSchoolId(String id, String schoolId);

    boolean existsByCycle_IdAndTeacherSubject_Id(String cycleId, String subjectId);

    Optional<AssessmentApprovalRequestEntity> findByCycle_IdAndTeacherSubject_Id(String cycleId, String subjectId);
}
