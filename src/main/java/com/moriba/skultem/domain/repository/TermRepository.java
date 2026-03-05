package com.moriba.skultem.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.model.Term.Status;

public interface TermRepository {
    void save(Term domain);

    Optional<Term> findById(String id);

    Optional<Term> findByIdAndSchoolId(String id, String school);

    Optional<Term> findActiveBySchoolAndAcademicYear(String school, String academicYearId);

    Optional<Term> findByIdAndAcademicYearIdAndSchoolId(String id, String academicYearId, String schoolId);

    Optional<Term> findByTernNumberAndAcademicYearIdAndSchoolId(int termNumber, String academicYearId, String schoolId);

    Optional<Term> findFirstBySchoolIdAndStatus(String schoolId, Status status);

    boolean existsByAcademicYearIdAndTermNumber(String academicYearId, int termNumber);

    List<Term> findByAcademicYearIdAndSchool(String academicYearId, String schoolId);

    boolean existsByNameAndSchoolId(String name, String schoolId);
   
    Page<Term> findAllBySchoolId(String schoolId, Pageable pageable);
    
    Page<Term> findAllBySchoolIdAndAcademicYear(String schoolId, String academicYear, Pageable pageable);

    void delete(Term domain);
}
