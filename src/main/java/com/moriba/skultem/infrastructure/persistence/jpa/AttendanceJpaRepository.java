package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moriba.skultem.application.dto.AttendanceHistoryDTO;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.AttendanceEntity;
import com.moriba.skultem.infrastructure.persistence.specs.FilterSpecificationBuilder;

public interface AttendanceJpaRepository
                extends JpaRepository<AttendanceEntity, String>, JpaSpecificationExecutor<AttendanceEntity> {
        Optional<AttendanceEntity> findByIdAndSchoolId(String id, String schoolId);

        Optional<AttendanceEntity> findByEnrollment_IdAndDateAndSchoolId(String enrollmentId, LocalDate date,
                        String schoolId);

        boolean existsByEnrollment_IdAndDateAndSchoolId(String enrollmentId, LocalDate date, String schoolId);

        Page<AttendanceEntity> findAllBySchoolId(String schoolId, Pageable pageable);

        Page<AttendanceEntity> findAllByEnrollment_IdAndSchoolId(String enrollmentId, String schoolId,
                        Pageable pageable);

        @Query("""
                            SELECT
                                a.date,
                                c.id,
                                c.name,
                                SUM(CASE WHEN a.present = true OR a.late = true THEN 1 ELSE 0 END),
                                COUNT(a),
                                MIN(a.createdAt),
                                MAX(a.updatedAt)
                            FROM AttendanceEntity a
                            JOIN a.enrollment e
                            JOIN e.clazz c
                            JOIN e.academicYear ac
                            WHERE a.schoolId = :schoolId
                              AND (:classId IS NULL OR c.id = :classId)
                              AND (:academicYearId IS NULL OR ac.id = :academicYearId)
                              AND (:startDate IS NULL OR a.date >= :startDate)
                              AND (:endDate IS NULL OR a.date <= :endDate)
                            GROUP BY a.date, c.id, c.name
                            ORDER BY a.date DESC
                        """)
        Page<AttendanceHistoryDTO> fetchDailyClassAttendanceSummary(
                        @Param("schoolId") String schoolId,
                        @Param("classId") String classId,
                        @Param("academicYearId") String academicYearId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        Pageable pageable);

        @Query("""
                                SELECT
                                    FUNCTION('TO_CHAR', a.date, 'Dy'),
                                    SUM(CASE WHEN a.present = true OR a.late = true THEN 1 ELSE 0 END),
                                    COUNT(a)
                                FROM AttendanceEntity a
                                WHERE a.schoolId = :schoolId
                                AND a.date BETWEEN :start AND :end
                                GROUP BY FUNCTION('TO_CHAR', a.date, 'Dy'), a.date
                                ORDER BY a.date
                        """)
        List<Object[]> weeklyAttendance(
                        String schoolId,
                        LocalDate start,
                        LocalDate end);

        // Per-student attendance counts for one class since a given date - backs the "needs
        // attention" flag (ComputeClassAttentionUseCase). "late" counts as attended, same
        // convention as fetchDailyClassAttendanceSummary/weeklyAttendance above; a holiday isn't
        // held against a student, so it's excluded from both the numerator and denominator.
        @Query("""
                            SELECT
                                e.id,
                                SUM(CASE WHEN a.present = true OR a.late = true THEN 1 ELSE 0 END),
                                COUNT(a)
                            FROM AttendanceEntity a
                            JOIN a.enrollment e
                            WHERE a.schoolId = :schoolId
                              AND e.clazz.id = :classId
                              AND e.academicYear.id = :academicYearId
                              AND a.date >= :since
                              AND a.holiday = false
                            GROUP BY e.id
                        """)
        List<Object[]> attendanceCountsByClassSince(
                        @Param("schoolId") String schoolId,
                        @Param("classId") String classId,
                        @Param("academicYearId") String academicYearId,
                        @Param("since") LocalDate since);

        // Per-student attendance counts for one class SESSION (class + section + stream) within an
        // explicit date range - backs Monthly/Term Summary and Inspection Reports. Same
        // "late counts as attended, holiday excluded" convention as attendanceCountsByClassSince
        // above, just parameterized by a date range and scoped to one session instead of the
        // whole class, so e.g. "SSS 1 Science" and "SSS 1 Art" report separately rather than
        // being merged into one "SSS 1" total.
        @Query("""
                            SELECT
                                e.id,
                                s.id,
                                s.givenNames,
                                s.familyName,
                                s.admissionNumber,
                                s.gender,
                                SUM(CASE WHEN a.present = true OR a.late = true THEN 1 ELSE 0 END),
                                SUM(CASE WHEN a.late = true THEN 1 ELSE 0 END),
                                COUNT(a)
                            FROM AttendanceEntity a
                            JOIN a.enrollment e
                            JOIN e.student s
                            WHERE a.schoolId = :schoolId
                              AND e.clazz.id = :classId
                              AND e.section.id = :sectionId
                              AND ((:streamId IS NULL AND e.stream IS NULL) OR e.stream.id = :streamId)
                              AND (:academicYearId IS NULL OR e.academicYear.id = :academicYearId)
                              AND a.date BETWEEN :startDate AND :endDate
                              AND a.holiday = false
                            GROUP BY e.id, s.id, s.givenNames, s.familyName, s.admissionNumber, s.gender
                        """)
        List<Object[]> attendanceCountsBySessionAndDateRange(
                        @Param("schoolId") String schoolId,
                        @Param("classId") String classId,
                        @Param("sectionId") String sectionId,
                        @Param("streamId") String streamId,
                        @Param("academicYearId") String academicYearId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        // Per-student attendance counts across EVERY class session in the school for one date
        // range - backs Class Summary. One row per (enrollment, i.e. student-in-a-session) rather
        // than pre-aggregated per class, so the use case can group by (classId, sectionId,
        // streamId) in Java and still compute per-class gender splits from the same rows.
        @Query("""
                            SELECT
                                e.clazz.id,
                                c.name,
                                e.section.id,
                                sec.name,
                                e.stream.id,
                                str.name,
                                e.id,
                                s.id,
                                s.gender,
                                SUM(CASE WHEN a.present = true OR a.late = true THEN 1 ELSE 0 END),
                                SUM(CASE WHEN a.late = true THEN 1 ELSE 0 END),
                                COUNT(a)
                            FROM AttendanceEntity a
                            JOIN a.enrollment e
                            JOIN e.student s
                            JOIN e.clazz c
                            JOIN e.section sec
                            LEFT JOIN e.stream str
                            WHERE a.schoolId = :schoolId
                              AND e.academicYear.id = :academicYearId
                              AND a.date BETWEEN :startDate AND :endDate
                              AND a.holiday = false
                            GROUP BY e.clazz.id, c.name, c.levelOrder, e.section.id, sec.name, e.stream.id,
                                str.name, e.id, s.id, s.gender
                            ORDER BY c.levelOrder, sec.name, str.name
                        """)
        List<Object[]> attendanceCountsBySchoolAndDateRange(
                        @Param("schoolId") String schoolId,
                        @Param("academicYearId") String academicYearId,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        List<AttendanceEntity> findAllByEnrollment_Clazz_IdAndEnrollment_Section_IdAndDateAndSchoolId(String classId,
                        String sectionId, LocalDate date, String schoolId);

        default Page<AttendanceEntity> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
                Specification<AttendanceEntity> spec = (root, query, cb) -> cb.equal(root.get("schoolId"), schoolId);

                if (filters != null && !filters.isEmpty()) {
                        spec = spec.and(FilterSpecificationBuilder.build(filters));
                }

                return findAll(spec, pageable);
        }
}
