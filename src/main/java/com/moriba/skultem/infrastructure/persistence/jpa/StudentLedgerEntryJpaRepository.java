package com.moriba.skultem.infrastructure.persistence.jpa;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.infrastructure.persistence.entity.StudentLedgerEntryEntity;

@Repository
public interface StudentLedgerEntryJpaRepository extends JpaRepository<StudentLedgerEntryEntity, String> {

        Optional<StudentLedgerEntryEntity> findTopBySchoolIdOrderByPaidAtDesc(String schoolId);

        Page<StudentLedgerEntryEntity> findAllByStudentIdAndSchoolId(String studentId, String schoolId,
                        Pageable pageable);

        Page<StudentLedgerEntryEntity> findAllByStudentIdAndAcademicYearIdAndSchoolId(String studentId,
                        String acadmicYearId, String schoolId, Pageable pageable);

        Page<StudentLedgerEntryEntity> findAllByAcademicYearIdAndSchoolId(String acadmicYearId, String schoolId,
                        Pageable pageable);

        Page<StudentLedgerEntryEntity> findAllBySchoolIdOrderByPaidAtDesc(String schoolId, Pageable pageable);

}
