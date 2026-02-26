package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.StudentLedgerEntry;

public interface StudentLedgerEntryRepository {
        void save(StudentLedgerEntry domain);

        Optional<StudentLedgerEntry> findTopBySchoolIdOrderByPaidAtDesc(String schoolId);

        Page<StudentLedgerEntry> findAllBySchoolIdOrderByPaidAtDesc(String schoolId, Pageable pageable);

        Page<StudentLedgerEntry> findAllByAcademicYearAndStudentAndSchool(String academicYearId, String studentId,
                        String schoolId,
                        Pageable pageable);

        Page<StudentLedgerEntry> findAllByAcademicYearAndSchool(String academicYearId, String schoolId,
                        Pageable pageable);

        Page<StudentLedgerEntry> findAllByStudentAndSchool(String studentId, String schoolId, Pageable pageable);

}
