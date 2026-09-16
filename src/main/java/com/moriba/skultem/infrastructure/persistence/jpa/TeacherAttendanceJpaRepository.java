package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.infrastructure.persistence.entity.TeacherAttendanceEntity;

public interface TeacherAttendanceJpaRepository extends JpaRepository<TeacherAttendanceEntity, String> {
    Optional<TeacherAttendanceEntity> findByTeacher_IdAndSchoolIdAndDate(String teacherId, String schoolId,
            LocalDate date);

    List<TeacherAttendanceEntity> findAllBySchoolIdAndDate(String schoolId, LocalDate date);

    List<TeacherAttendanceEntity> findAllBySchoolId(String schoolId);

    List<TeacherAttendanceEntity> findAllBySchoolIdAndTeacher_IdAndDateBetween(String schoolId, String teacherId,
            LocalDate from, LocalDate to);

    // Per-teacher attendance counts across the whole school for one date range - backs Monthly/
    // Term Summary and Management Reports. Only teachers with at least one recorded day appear;
    // the use case merges this against the full active-teacher roster so teachers with zero
    // records still show up (a "missing attendance" signal, not silently dropped). EXCUSED folds
    // into "absent" - no separate column, matching the confirmed student-side convention. Row
    // shape: [teacherId, presentCount (Long), lateCount (Long), absentOrExcusedCount (Long),
    // totalCount (Long)].
    @Query("""
                        SELECT
                            t.id,
                            SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END),
                            SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END),
                            SUM(CASE WHEN a.status = 'ABSENT' OR a.status = 'EXCUSED' THEN 1 ELSE 0 END),
                            COUNT(a)
                        FROM TeacherAttendanceEntity a
                        JOIN a.teacher t
                        WHERE a.schoolId = :schoolId
                          AND a.date BETWEEN :startDate AND :endDate
                        GROUP BY t.id
                    """)
    List<Object[]> attendanceCountsByTeacherAndDateRange(
                    @Param("schoolId") String schoolId,
                    @Param("startDate") LocalDate startDate,
                    @Param("endDate") LocalDate endDate);
}
