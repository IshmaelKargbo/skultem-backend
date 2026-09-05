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
    // query matches on subject or teacher name - blank skips it. Checked against '' rather than
    // NULL (unlike :status below, a properly-typed enum) - a null String bound into a
    // lower(concat(...)) call leaves Postgres/the JDBC driver unable to infer its type from
    // context and it falls back to bytea ("function lower(bytea) does not exist"), so
    // ListAssessmentApprovalRequestUseCase.normalizeQuery always sends a real (possibly empty)
    // string, never null.
    @Query(value = """
                SELECT aar
                FROM AssessmentApprovalRequestEntity aar
                JOIN aar.teacherSubject ts
                JOIN ts.session s
                JOIN s.academicYear ac
                JOIN ts.subject subj
                JOIN ts.teacher t
                JOIN t.user u
                JOIN ClassMasterEntity cm ON cm.session = s
                WHERE cm.teacher.id = :teacherId
                  AND ac.id = :academicYearId
                  AND cm.endedAt IS NULL
                  AND (:status IS NULL OR aar.status = :status)
                  AND (:query = ''
                       OR lower(subj.name) LIKE lower(concat('%', :query, '%'))
                       OR lower(u.givenName) LIKE lower(concat('%', :query, '%'))
                       OR lower(u.familyName) LIKE lower(concat('%', :query, '%')))
            """, countQuery = """
                SELECT COUNT(aar)
                FROM AssessmentApprovalRequestEntity aar
                JOIN aar.teacherSubject ts
                JOIN ts.session s
                JOIN s.academicYear ac
                JOIN ts.subject subj
                JOIN ts.teacher t
                JOIN t.user u
                JOIN ClassMasterEntity cm ON cm.session = s
                WHERE cm.teacher.id = :teacherId
                  AND ac.id = :academicYearId
                  AND cm.endedAt IS NULL
                  AND (:status IS NULL OR aar.status = :status)
                  AND (:query = ''
                       OR lower(subj.name) LIKE lower(concat('%', :query, '%'))
                       OR lower(u.givenName) LIKE lower(concat('%', :query, '%'))
                       OR lower(u.familyName) LIKE lower(concat('%', :query, '%')))
            """)
    Page<AssessmentApprovalRequestEntity> findAllForClassMasterByTeacherId(
            @Param("teacherId") String teacherId,
            @Param("academicYearId") String academicYearId,
            @Param("status") AssessmentApprovalRequest.Status status,
            @Param("query") String query,
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

    // School-wide, unlike the teacher-scoped query above - backs the admin approval view's
    // default "everything pending across the school" list, so an admin isn't forced to already
    // know which teacher/class to check before anything shows.
    @Query(value = """
                SELECT aar
                FROM AssessmentApprovalRequestEntity aar
                JOIN aar.teacherSubject ts
                JOIN ts.session s
                JOIN s.academicYear ac
                JOIN ts.subject subj
                JOIN ts.teacher t
                JOIN t.user u
                WHERE aar.schoolId = :schoolId
                  AND ac.id = :academicYearId
                  AND (:status IS NULL OR aar.status = :status)
                  AND (:query = ''
                       OR lower(subj.name) LIKE lower(concat('%', :query, '%'))
                       OR lower(u.givenName) LIKE lower(concat('%', :query, '%'))
                       OR lower(u.familyName) LIKE lower(concat('%', :query, '%')))
            """, countQuery = """
                SELECT COUNT(aar)
                FROM AssessmentApprovalRequestEntity aar
                JOIN aar.teacherSubject ts
                JOIN ts.session s
                JOIN s.academicYear ac
                JOIN ts.subject subj
                JOIN ts.teacher t
                JOIN t.user u
                WHERE aar.schoolId = :schoolId
                  AND ac.id = :academicYearId
                  AND (:status IS NULL OR aar.status = :status)
                  AND (:query = ''
                       OR lower(subj.name) LIKE lower(concat('%', :query, '%'))
                       OR lower(u.givenName) LIKE lower(concat('%', :query, '%'))
                       OR lower(u.familyName) LIKE lower(concat('%', :query, '%')))
            """)
    Page<AssessmentApprovalRequestEntity> findAllBySchool(
            @Param("schoolId") String schoolId,
            @Param("academicYearId") String academicYearId,
            @Param("status") AssessmentApprovalRequest.Status status,
            @Param("query") String query,
            Pageable pageable);

    @Query("""
                SELECT COUNT(aar)
                FROM AssessmentApprovalRequestEntity aar
                JOIN aar.teacherSubject ts
                JOIN ts.session s
                JOIN s.academicYear ac
                WHERE aar.schoolId = :schoolId
                  AND ac.id = :academicYearId
                  AND aar.status = :status
            """)
    long countBySchoolAndStatus(
            @Param("schoolId") String schoolId,
            @Param("academicYearId") String academicYearId,
            @Param("status") AssessmentApprovalRequest.Status status);

    Optional<AssessmentApprovalRequestEntity> findByIdAndSchoolId(String id, String schoolId);

    boolean existsByCycle_IdAndTeacherSubject_Id(String cycleId, String subjectId);

    Optional<AssessmentApprovalRequestEntity> findByCycle_IdAndTeacherSubject_Id(String cycleId, String subjectId);
}
