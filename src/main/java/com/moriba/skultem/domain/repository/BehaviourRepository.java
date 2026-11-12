package com.moriba.skultem.domain.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Behaviour;
import com.moriba.skultem.domain.vo.KindCount;

public interface BehaviourRepository {
    // A student's most recent records, newest first (Skully).
    List<Behaviour> findRecentByStudent(String schoolId, String studentId, int limit);

    void save(Behaviour domain);

    boolean existsByEnrollmentIdAndSchoolId(String enrollmentId, String schoolId);

    List<KindCount> countByKindForClassOrAll(String academicYearId, String schoolId, String classId,
            java.util.Collection<com.moriba.skultem.domain.vo.Level> levels);

    Page<Behaviour> findAllAcademicYearAndSchoolId(String academicYearId, String schoolId,
            java.util.Collection<com.moriba.skultem.domain.vo.Level> levels, Pageable pageable);

    Page<Behaviour> findAllAcademicYearAndClassIdAndSchoolId(String academicYearId, String classId, String schoolId,
            java.util.Collection<com.moriba.skultem.domain.vo.Level> levels, Pageable pageable);
}
