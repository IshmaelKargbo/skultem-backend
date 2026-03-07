package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Enrollment;
import com.moriba.skultem.domain.repository.EnrollmentRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.EnrollmentJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.EnrollmentMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EnrollementAdapter implements EnrollmentRepository {
    private final EnrollmentJpaRepository repo;

    @Override
    public void save(Enrollment domain) {
        var entity = EnrollmentMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Enrollment> findById(String id) {
        return repo.findById(id).map(EnrollmentMapper::toDomain);
    }

    @Override
    public Optional<Enrollment> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(EnrollmentMapper::toDomain);
    }

    @Override
    public boolean existsByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndStreamIdAndSchoolId(String studentId,
            String classId, String sectionId, String academicYearId, String streamId, String schoolId) {
        return repo.existsByStudent_IdAndClazz_IdAndSection_IdAndAcademicYear_IdAndStream_IdAndSchoolId(studentId,
                classId,
                sectionId, academicYearId, streamId, schoolId);
    }

    @Override
    public boolean existsByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndSchoolIdAndStreamIdIsNull(
            String studentId,
            String classId, String sectionId, String academicYearId, String schoolId) {
        return repo.existsByStudent_IdAndClazz_IdAndSection_IdAndAcademicYear_IdAndSchoolIdAndStreamIsNull(studentId,
                classId, sectionId, academicYearId, schoolId);
    }

    @Override
    public Page<Enrollment> findBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(EnrollmentMapper::toDomain);
    }

    @Override
    public long countAll() {
        return repo.count();
    }

    @Override
    public boolean existsByStudentIdAndAcademicYearIdAndSchoolId(String studentId, String academicYearId,
            String schoolId) {
        return repo.existsByStudent_IdAndAcademicYear_IdAndSchoolId(studentId, academicYearId, schoolId);
    }

    @Override
    public Optional<Enrollment> findByStudentAndAcademicYearAndSchoolId(String studentId, String academicYearId,
            String schoolId) {
        return repo.findByStudent_IdAndAcademicYear_IdAndSchoolId(studentId, academicYearId, schoolId)
                .map(EnrollmentMapper::toDomain);
    }

    @Override
    public Optional<Enrollment> findByClassAndStudentAndSchoolId(String classId, String studentId, String schoolId) {
        return repo.findByClazz_IdAndStudent_IdAndSchoolId(classId, studentId, schoolId)
                .map(EnrollmentMapper::toDomain);
    }

    @Override
    public Page<Enrollment> findAllByClassAndAcademicSchoolId(String classId, String academicYearId, String schoolId,
            Pageable pageable) {
        return repo.findAllByClazz_IdAndAcademicYear_IdAndSchoolId(classId, academicYearId, schoolId, pageable)
                .map(EnrollmentMapper::toDomain);
    }

    @Override
    public List<Enrollment> findAllByAcademicSchoolId(String academicYearId, String schoolId) {
        return repo.findAllByAcademicYear_IdAndSchoolId(academicYearId, schoolId).stream()
                .map(EnrollmentMapper::toDomain).toList();
    }

    @Override
    public Page<Enrollment> findAllByClassAndSchoolId(String classId, String schoolId, Pageable pageable) {
        return repo.findAllByClazz_IdAndSchoolId(classId, schoolId, pageable).map(EnrollmentMapper::toDomain);
    }

    @Override
    public List<Enrollment> findAllByStudentIdAndClassIdAndSectionIdAndAcademicYearIdAndStreamIdAndSchoolId(
            String studentId, String classId, String sectionId, String academicYearId, String streamId,
            String schoolId) {
        return repo
                .findAllByStudentIdAndClazz_IdAndSection_IdAndAcademicYear_IdAndStream_IdAndSchoolId(studentId, classId,
                        sectionId, academicYearId, streamId, schoolId)
                .stream()
                .map(EnrollmentMapper::toDomain).toList();
    }

    @Override
    public List<Enrollment> findAllByStreamIdAndAcademicYearIdAndSchoolId(String stream, String academicYearId,
            String schoolId) {
        return repo
                .findAllByStream_IdAndAcademicYear_IdAndSchoolId(stream, academicYearId, schoolId)
                .stream()
                .map(EnrollmentMapper::toDomain).toList();
    }

}
