package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.StudentLedgerEntry;
import com.moriba.skultem.domain.repository.StudentLedgerEntryRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.StudentLedgerEntryJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.StudentLedgerEntryMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StudentLedgerEntryAdapter implements StudentLedgerEntryRepository {

    private final StudentLedgerEntryJpaRepository repo;

    @Override
    public void save(StudentLedgerEntry domain) {
        var entity = StudentLedgerEntryMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Page<StudentLedgerEntry> findAllBySchoolIdOrderByPaidAtDesc(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByPaidAtDesc(schoolId, pageable).map(StudentLedgerEntryMapper::toDomain);
    }

    @Override
    public Page<StudentLedgerEntry> findAllByAcademicYearAndStudentAndSchool(String academicYearId, String studentId,
            String schoolId,
            Pageable pageable) {
        return repo.findAllByStudentIdAndAcademicYearIdAndSchoolId(studentId, academicYearId, schoolId, pageable)
                .map(StudentLedgerEntryMapper::toDomain);
    }

    @Override
    public Page<StudentLedgerEntry> findAllByStudentAndSchool(String studentId, String schoolId, Pageable pageable) {
        return repo.findAllByStudentIdAndSchoolId(studentId, schoolId, pageable)
                .map(StudentLedgerEntryMapper::toDomain);
    }

    @Override
    public Page<StudentLedgerEntry> findAllByAcademicYearAndSchool(String academicYearId, String schoolId,
            Pageable pageable) {
        return repo.findAllByAcademicYearIdAndSchoolId(academicYearId, schoolId, pageable)
                .map(StudentLedgerEntryMapper::toDomain);
    }

    @Override
    public Optional<StudentLedgerEntry> findTopBySchoolIdOrderByPaidAtDesc(String schoolId) {
        return repo.findTopBySchoolIdOrderByPaidAtDesc(schoolId).map(StudentLedgerEntryMapper::toDomain);
    }
}
