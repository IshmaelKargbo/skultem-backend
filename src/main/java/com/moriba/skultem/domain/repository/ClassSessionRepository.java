package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.vo.Level;

import java.util.Collection;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.ClassSession;
import com.moriba.skultem.domain.vo.Filter;

public interface ClassSessionRepository {
        void save(ClassSession domain);

        void saveAll(List<ClassSession> domains);

        Optional<ClassSession> findByIdAndSchoolId(String id, String schoolId);

        Optional<ClassSession> findByAcademicYearAndClassAndSchoolId(String academic, String classId, String schoolId);

        boolean existsByClassIdAndAcademicYearIdAndStreamIdAndSchoolId(
                        String classId, String academicYearId, String streamId, String schoolId);

        boolean existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(
                        String classId, String academicYearId, String sectionId, String streamId, String schoolId);

        boolean existsByClassIdAndAcademicYearIdAndSectionIdAndStreamIsNullAndSchoolId(
                        String classId, String academicYearId, String sectionId, String schoolId);

        Page<ClassSession> findUnassignedBySchoolAndAcademicYear(String schoolId, String academicYearId,
                        Collection<Level> levels, Pageable pageable);

        Page<ClassSession> findBySchoolId(String schoolId, Pageable pageable);

        Page<ClassSession> findBySchoolIdAndAcademicYearId(String schoolId, String academicYearId, Pageable pageable);

        // Only sessions whose class is at one of these levels (see SectionScope).
        Page<ClassSession> findBySchoolIdAndAcademicYearId(String schoolId, String academicYearId,
                        Collection<Level> levels, Pageable pageable);

        // Matches on class/section/stream name, optionally narrowed to one section, stream and/or
        // class level (the Level enum's name, '' for any) - backs the classes list's search box and
        // filters.
        Page<ClassSession> search(String schoolId, String academicYearId, String sectionId, String streamId,
                        String level, String query, Collection<Level> levels, Pageable pageable);

        Optional<ClassSession> findByClassIdAndAcademicYearIdAndSchoolId(String classId, String academicYearId,
                        String schoolId);

        Optional<ClassSession> findByClassIdAndStreamIdAndAcademicYearId(String classId, String streamId, String academicYearId);

        Optional<ClassSession> findByClassIdAndAcademicYearIdAndSectionIdAndSchoolId(String classId,
                        String academicYearId, String sectionId, String schoolId);

        Optional<ClassSession> findByClassIdAndAcademicYearIdAndSectionIdAndStreamIdAndSchoolId(String classId,
                        String academicYearId, String sectionId, String streamId, String schoolId);

        List<ClassSession> findAllByClassIdAndAcademicYearIdAndSchoolId(String classId, String academicYearId,
                        String schoolId);

        long countBySchoolId(String schoolId);

        long countAll();

        // levels: always applied (full catalog for whole-school callers) - see SectionScope.
        Page<ClassSession> runReport(String schoolId, List<Filter> filters, Collection<Level> levels,
                Pageable pageable);
}
