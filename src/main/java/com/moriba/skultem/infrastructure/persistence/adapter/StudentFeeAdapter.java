package com.moriba.skultem.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.StudentFee;
import com.moriba.skultem.domain.repository.StudentFeeRepository;
import com.moriba.skultem.domain.vo.Filter;
import com.moriba.skultem.infrastructure.persistence.entity.StudentFeeEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.StudentFeeJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.StudentFeeMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StudentFeeAdapter implements StudentFeeRepository {

    private final StudentFeeJpaRepository repo;

    @Override
    public void save(StudentFee domain) {
        var entity = StudentFeeMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public void saveAll(List<StudentFee> records) {
        List<StudentFeeEntity> fees = records.stream().map(StudentFeeMapper::toEntity).toList();
        repo.saveAll(fees);
    }

    @Override
    public Optional<StudentFee> findByIdAndSchoolId(String id) {
        return repo.findByIdAndSchoolId(id, id).map(StudentFeeMapper::toDomain);
    }

    @Override
    public boolean existsBySchoolAndEnrollmentAndStudentAndFee(String schoolId, String enrollmentId, String studentId,
            String feeId) {
        return repo.existsByEnrollment_IdAndFee_IdAndStudent_IdAndSchoolId(enrollmentId, feeId, studentId, schoolId);
    }

    @Override
    public Page<StudentFee> findBySchool(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(StudentFeeMapper::toDomain);
    }

    @Override
    public Page<StudentFee> findAllBySchoolAndStudent(String schoolId, String student, Pageable pageable) {
        return repo.findAllByStudent_IdAndSchoolId(student, schoolId, pageable).map(StudentFeeMapper::toDomain);
    }

    @Override
    public Page<StudentFee> findBySchoolAndEnrollment(String schoolId, String enrollmentId, Pageable pageable) {
        return repo.findAllByEnrollment_IdAndSchoolId(enrollmentId, schoolId, pageable).map(StudentFeeMapper::toDomain);
    }

    @Override
    public Page<StudentFee> findBySchoolAndEnrollmentAndFee(String schoolId, String enrollmentId, String feeId,
            Pageable pageable) {
        return repo.findAllByFee_IdAndEnrollment_IdAndSchoolId(feeId, enrollmentId, schoolId, pageable)
                .map(StudentFeeMapper::toDomain);
    }

    @Override
    public Page<StudentFee> findBySchoolAndEnrollmentAndStudent(String schoolId, String enrollmentId, String classId,
            Pageable pageable) {
        return repo.findAllByEnrollment_IdAndStudent_IdAndSchoolId(enrollmentId, classId, schoolId, pageable)
                .map(StudentFeeMapper::toDomain);
    }

    @Override
    public long countByFeeAndSchool(String feeId, String schoolId) {
        return repo.countByFee_IdAndSchoolId(feeId, schoolId);
    }

    @Override
    public BigDecimal sumTotalFeeByStudent(String studentId, String schoolId) {
        return repo.sumTotalFeeByStudent(studentId, schoolId);
    }

    @Override
    public Page<StudentFee> runReport(String schoolId, List<Filter> filters, Pageable pageable) {
        return repo.runReport(schoolId, filters, pageable)
                .map(StudentFeeMapper::toDomain);
    }

}
