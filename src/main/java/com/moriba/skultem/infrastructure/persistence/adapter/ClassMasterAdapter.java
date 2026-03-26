package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ClassMaster;
import com.moriba.skultem.domain.repository.ClassMasterRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ClassMasterJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ClassMasterMapper;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClassMasterAdapter implements ClassMasterRepository {
    private final ClassMasterJpaRepository repo;

    @Override
    public void save(ClassMaster domain) {
        var entity = ClassMasterMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<ClassMaster> findById(String id) {
        return repo.findById(id).map(ClassMasterMapper::toDomain);
    }

    @Override
    public boolean existsByTeacherIdAndClassSessionIdAndSchoolId(String teacherId, String classSessionId,
            String schoolId) {
        return repo.existsBySession_IdAndTeacher_IdAndSchoolId(classSessionId, teacherId, schoolId);
    }

    @Override
    public Page<ClassMaster> findBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(ClassMasterMapper::toDomain);
    }

    @Override
    public long countAll() {
        return repo.count();
    }

    @Override
    public boolean existsByClassSessionIdAndSchoolId(String classSessionId, String schoolId) {
        return repo.existsBySession_IdAndSchoolId(classSessionId, schoolId);
    }

    @Override
    public Optional<ClassMaster> findTopByClassSessionIdAndEndedAtIsNullOrderByAssignedAtDesc(String sessionId) {
        return repo.findTopBySession_IdAndEndedAtIsNullOrderByAssignedAtDesc(sessionId)
                .map(ClassMasterMapper::toDomain);
    }

    @Override
    public Optional<ClassMaster> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(ClassMasterMapper::toDomain);
    }

    @Override
    public Optional<ClassMaster> findBySessionIdAndSchoolId(String sessionId, String schoolId) {
        return repo.findAllBySession_IdAndSchoolId(sessionId, schoolId)
                .map(ClassMasterMapper::toDomain);
    }

    @Override
    public Page<ClassMaster> findByTeacherAndAcademicYear(String teacherId, String academicYearId, Pageable pageable) {
        return repo.findAllByTeacher_IdAndSession_AcademicYear_Id(teacherId, academicYearId, pageable)
                .map(ClassMasterMapper::toDomain);
    }

}
