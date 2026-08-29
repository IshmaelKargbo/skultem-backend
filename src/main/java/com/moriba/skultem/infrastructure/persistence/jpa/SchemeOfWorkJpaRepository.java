package com.moriba.skultem.infrastructure.persistence.jpa;

import com.moriba.skultem.infrastructure.persistence.entity.SchemeOfWorkEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SchemeOfWorkJpaRepository extends JpaRepository<SchemeOfWorkEntity, String> {
    boolean existsByTermIdAndSubjectIdAndSessionIdAndSchoolId(String termId, String subjectId, String sessionId, String schoolId);

    Optional<SchemeOfWorkEntity> findByTermIdAndSubjectIdAndSessionIdAndSchoolId(String termId, String subjectId, String sessionId, String schoolId);

    Page<SchemeOfWorkEntity> findAllBySchoolId(String school, Pageable pageable);

    @Query("""
                SELECT s FROM SchemeOfWorkEntity s
                WHERE s.schoolId = :schoolId
                AND EXISTS (
                    SELECT 1 FROM TeacherSubjectEntity ts
                    WHERE ts.teacher.id = :teacherId
                    AND ts.subject.id = s.subject.id
                    AND ts.session.id = s.session.id
                )
            """)
    Page<SchemeOfWorkEntity> findAllByTeacherIdAndSchoolId(
            @Param("teacherId") String teacherId,
            @Param("schoolId") String schoolId,
            Pageable pageable);

    // The progress filter matches a scheme's rolled-up week progress (see Week.deriveProgress):
    // COMPLETED = has weeks and none are anything but COMPLETED; NOT_STARTED = no week has been
    // started (including schemes with no weeks yet); IN_PROGRESS = anything else.
    //
    // The filter used to be a single `:progress` parameter compared directly against
    // com.moriba.skultem.domain.model.Week.State enum literals (e.g. `:progress = ...COMPLETED`).
    // Hibernate could resolve those literals fine when compared against a mapped attribute
    // (w.state <> ...COMPLETED), but a parameter compared *only* against a bare enum literal has
    // no other context to infer its type from, and threw "Could not determine ValueMapping for
    // SqmParameter" on every call - filtered or not, since the OR chain is always parsed as a
    // whole. Booleans avoid the ambiguity entirely: SchemeOfWorkAdapter derives one boolean per
    // branch from the enum, so every parameter here has an unambiguous, always-inferable type.
    @Query("""
                SELECT s FROM SchemeOfWorkEntity s
                WHERE s.schoolId = :schoolId
                AND (:subjectId IS NULL OR s.subject.id = :subjectId)
                AND (:sessionId IS NULL OR s.session.id = :sessionId)
                AND (:termId IS NULL OR s.term.id = :termId)
                AND (
                    :hasProgressFilter = false
                    OR (:matchCompleted = true
                        AND EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id)
                        AND NOT EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week$State.COMPLETED))
                    OR (:matchNotStarted = true
                        AND NOT EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week$State.NOT_STARTED))
                    OR (:matchInProgress = true
                        AND EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week$State.NOT_STARTED)
                        AND EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week$State.COMPLETED))
                )
                ORDER BY s.createdAt DESC
            """)
    Page<SchemeOfWorkEntity> search(
            @Param("schoolId") String schoolId,
            @Param("subjectId") String subjectId,
            @Param("sessionId") String sessionId,
            @Param("termId") String termId,
            @Param("hasProgressFilter") boolean hasProgressFilter,
            @Param("matchCompleted") boolean matchCompleted,
            @Param("matchNotStarted") boolean matchNotStarted,
            @Param("matchInProgress") boolean matchInProgress,
            Pageable pageable);

    @Query("""
                SELECT s FROM SchemeOfWorkEntity s
                WHERE s.schoolId = :schoolId
                AND (:subjectId IS NULL OR s.subject.id = :subjectId)
                AND (:sessionId IS NULL OR s.session.id = :sessionId)
                AND (:termId IS NULL OR s.term.id = :termId)
                AND (
                    :hasProgressFilter = false
                    OR (:matchCompleted = true
                        AND EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id)
                        AND NOT EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week$State.COMPLETED))
                    OR (:matchNotStarted = true
                        AND NOT EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week$State.NOT_STARTED))
                    OR (:matchInProgress = true
                        AND EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week$State.NOT_STARTED)
                        AND EXISTS (SELECT 1 FROM WeekEntity w WHERE w.scheme.id = s.id AND w.state <> com.moriba.skultem.domain.model.Week$State.COMPLETED))
                )
                AND EXISTS (
                    SELECT 1 FROM TeacherSubjectEntity ts
                    WHERE ts.teacher.id = :teacherId
                    AND ts.subject.id = s.subject.id
                    AND ts.session.id = s.session.id
                )
                ORDER BY s.createdAt DESC
            """)
    Page<SchemeOfWorkEntity> searchByTeacher(
            @Param("teacherId") String teacherId,
            @Param("schoolId") String schoolId,
            @Param("subjectId") String subjectId,
            @Param("sessionId") String sessionId,
            @Param("termId") String termId,
            @Param("hasProgressFilter") boolean hasProgressFilter,
            @Param("matchCompleted") boolean matchCompleted,
            @Param("matchNotStarted") boolean matchNotStarted,
            @Param("matchInProgress") boolean matchInProgress,
            Pageable pageable);
}
