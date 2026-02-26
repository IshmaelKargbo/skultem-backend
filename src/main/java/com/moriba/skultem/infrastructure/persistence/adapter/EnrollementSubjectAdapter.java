package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.EnrollmentSubject;
import com.moriba.skultem.domain.repository.EnrollmentSubjectRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.EnrollmentSubjectJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.EnrollmentSubjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EnrollementSubjectAdapter implements EnrollmentSubjectRepository {
    private final EnrollmentSubjectJpaRepository repo;

    @Override
    public void save(EnrollmentSubject domain) {
        var entity = EnrollmentSubjectMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<EnrollmentSubject> findById(String id) {
        return repo.findById(id).map(EnrollmentSubjectMapper::toDomain);
    }

    @Override
    public boolean existsByEnrollmentIdAndSubjectIdAndSchoolId(String enrollmentId, String subjectId, String schoolId) {
        return repo.existsByEnrollment_IdAndSubject_IdAndSchoolId(enrollmentId, subjectId, schoolId);
    }

    @Override
    public Page<EnrollmentSubject> findBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(EnrollmentSubjectMapper::toDomain);
    }

    @Override
    public long countByEnrollmentIdAndSchoolId(String enrollmentId, String schoolId) {
        return repo.countByEnrollment_IdAndSchoolId(enrollmentId, schoolId);
    }

    @Override
    public long countAll() {
        return repo.count();
    }

    @Override
    public List<EnrollmentSubject> findAllByEnrollmentIdAndSchoolId(String enrollmentId, String schoolId) {
        return repo.findAllByEnrollment_IdAndSchoolId(enrollmentId, schoolId).stream()
                .map(EnrollmentSubjectMapper::toDomain).toList();
    }

    @Override
    public void delete(EnrollmentSubject domain) {
        var entity = EnrollmentSubjectMapper.toEntity(domain);
        repo.delete(entity);
    }

}
