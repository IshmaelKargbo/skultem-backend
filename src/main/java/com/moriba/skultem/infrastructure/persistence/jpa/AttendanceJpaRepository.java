package com.moriba.skultem.infrastructure.persistence.jpa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.application.dto.AttendanceHistoryDTO;
import com.moriba.skultem.infrastructure.persistence.entity.AttendanceEntity;

@Repository
public interface AttendanceJpaRepository extends JpaRepository<AttendanceEntity, String> {
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
                                COUNT(a)
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

        List<AttendanceEntity> findAllByEnrollment_Clazz_IdAndEnrollment_Section_IdAndDateAndSchoolId(String classId,
                        String sectionId, LocalDate date, String schoolId);
}
