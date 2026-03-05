package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.TeacherSubject;

public interface TeacherSubjectRepository {

        void save(TeacherSubject domain);

        void deleteByClassIdAndSubjectIdAndSchoolId(String classId, String subjectId, String schoolId);

        void deleteByStreamIdAndSubjectIdAndSchoolId(String streamId, String subjectId, String schoolId);

        Optional<TeacherSubject> findById(String id);

        Optional<TeacherSubject> findByIdAndSchoolId(String id, String schoolId);

        Optional<TeacherSubject> findOneByTeacherIdAndSchoolId(String teacherId, String schoolId);

        boolean existsByTeacherIdAndClassSessionIdAndSubjectIdAndSchoolId(
                        String teacherId, String classSessionId, String subjectId, String schoolId);

        boolean existsByClassSessionIdAndSubjectIdAndSchoolId(String sessionId, String subjectId, String schoolId);

        Page<TeacherSubject> findAllBySchoolId(String schoolId, Pageable pageable);

        Page<TeacherSubject> findAllBySchoolIdAndAcademicYearId(String schoolId, String academicYearId,
                        Pageable pageable);

        Page<TeacherSubject> findByTeacherId(String teacherId, Pageable pageable);

        Page<TeacherSubject> findAllByTeacherIdAndClassSessionId(
                        String teacherId, String classSessionId, Pageable pageable);

        List<TeacherSubject> findByTeacherIdAndSchoolId(String teacherId, String schoolId);

        Optional<TeacherSubject> findBySubjectIdAndSessionIdAndSchoolId(String subjectId, String academicYearId, String schoolId);

        Page<TeacherSubject> findByClassSessionIdAndSchoolId(String sessionId, String schoolId, Pageable pageable);

        void delete(TeacherSubject domain);
}
