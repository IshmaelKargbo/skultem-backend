package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.TeacherSubject;
import com.moriba.skultem.domain.repository.TeacherSubjectRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.TeacherSubjectJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.TeacherSubjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TeacherSubjectAdapter implements TeacherSubjectRepository {
    private final TeacherSubjectJpaRepository repo;

    @Override
    public void save(TeacherSubject domain) {
        var entity = TeacherSubjectMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<TeacherSubject> findById(String id) {
        return repo.findById(id).map(TeacherSubjectMapper::toDomain);
    }

    @Override
    public boolean existsByTeacherIdAndClassSessionIdAndSubjectIdAndSchoolId(String teacherId, String classSessionId,
            String subjectId, String schoolId) {
        return repo.existsByTeacher_IdAndSession_IdAndSubject_IdAndSchoolId(teacherId, classSessionId, subjectId,
                schoolId);
    }

    @Override
    public Page<TeacherSubject> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(TeacherSubjectMapper::toDomain);
    }

    @Override
    public Page<TeacherSubject> findByTeacherIdAndClassSessionId(String teacherId, String classSessionId,
            Pageable pageable) {
        return repo.findAllByTeacher_IdAndSessionId(teacherId, classSessionId, pageable)
                .map(TeacherSubjectMapper::toDomain);
    }

    @Override
    public Page<TeacherSubject> findByClassSessionIdAndSchoolId(String sessionId, String schoolId, Pageable pageable) {
        return repo.findAllBySessionIdAndSchoolId(sessionId, schoolId, pageable).map(TeacherSubjectMapper::toDomain);
    }

    @Override
    public Page<TeacherSubject> findByTeacherId(String teacherId, Pageable pageable) {
        return repo.findAllByTeacherId(teacherId, pageable).map(TeacherSubjectMapper::toDomain);
    }

    @Override
    public boolean existsByClassSessionIdAndSubjectIdAndSchoolId(String sessionId, String subjectId, String schoolId) {
        return repo.existsBySession_IdAndSubject_IdAndSchoolId(sessionId, subjectId, schoolId);
    }

    @Override
    public List<TeacherSubject> findByTeacherIdAndSchoolId(String teacherId, String schoolId) {
        return repo.findByTeacher_IdAndSchoolId(teacherId, schoolId).stream().map(TeacherSubjectMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<TeacherSubject> findOneByTeacherIdAndSchoolId(String teacherId, String schoolId) {
        return repo.findOneByTeacher_IdAndSchoolId(teacherId, schoolId).map(TeacherSubjectMapper::toDomain);
    }

    @Override
    public void delete(TeacherSubject domain) {
        var entity = TeacherSubjectMapper.toEntity(domain);
        repo.delete(entity);
    }

    @Override
    public Page<TeacherSubject> findAllBySchoolIdAndAcademicYearId(String schoolId, String academicYearId,
            Pageable pageable) {
        return repo.findAllBySession_AcademicYear_IdAndSchoolId(academicYearId, schoolId, pageable)
                .map(TeacherSubjectMapper::toDomain);
    }
}
