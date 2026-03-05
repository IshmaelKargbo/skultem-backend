package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.model.Term.Status;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.infrastructure.persistence.entity.TermEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.TermJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.TermMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TermAdapter implements TermRepository {

    private final TermJpaRepository repo;

    @Override
    public void save(Term domain) {
        TermEntity entity = TermMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Term> findById(String id) {
        return repo.findById(id).map(TermMapper::toDomain);
    }

    @Override
    public boolean existsByNameAndSchoolId(String schoolId, String name) {
        return repo.existsByNameIgnoreCaseAndSchoolId(name, schoolId);
    }

    @Override
    public Page<Term> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByTermNumberAsc(schoolId, pageable).map(TermMapper::toDomain);
    }

    @Override
    public void delete(Term domain) {
        TermEntity entity = TermMapper.toEntity(domain);
        repo.delete(entity);
    }

    @Override
    public Page<Term> findAllBySchoolIdAndAcademicYear(String schoolId, String academicYear, Pageable pageable) {
        return repo.findAllByAcademicYearIdAndSchoolIdOrderByTermNumberAsc(academicYear, schoolId, pageable).map(TermMapper::toDomain);
    }

    @Override
    public boolean existsByAcademicYearIdAndTermNumber(String academicYearId, int termNumber) {
        return repo.existsByAcademicYearIdAndTermNumber(academicYearId, termNumber);
    }

    @Override
    public Optional<Term> findByIdAndSchoolId(String id, String school) {
        return repo.findByIdAndSchoolId(id, school).map(TermMapper::toDomain);
    }

    @Override
    public List<Term> findByAcademicYearIdAndSchool(String academicYearId, String schoolId) {
        return repo.findAllByAcademicYearIdAndSchoolIdOrderByTermNumberAsc(academicYearId, schoolId).stream().map(TermMapper::toDomain).toList();
    }

    @Override
    public Optional<Term> findByIdAndAcademicYearIdAndSchoolId(String id, String academicYearId, String schoolId) {
        return repo.findByIdAndAcademicYear_IdAndSchoolId(id, academicYearId, schoolId).map(TermMapper::toDomain);
    }

    @Override
    public Optional<Term> findFirstBySchoolIdAndStatus(String schoolId, Status status) {
        return repo.findFirstBySchoolIdAndStatusOrderByTermNumberAsc(schoolId, status).map(TermMapper::toDomain);
    }

    @Override
    public Optional<Term> findByTernNumberAndAcademicYearIdAndSchoolId(int termNumber, String academicYearId, String schoolId) {
        return repo.findByTermNumberAndAcademicYear_IdAndSchoolId(termNumber, academicYearId, schoolId).map(TermMapper::toDomain);
    }

    @Override
    public Optional<Term> findActiveBySchoolAndAcademicYear(String school, String academicYearId) {
        return repo.findActiveTerm(academicYearId, school).map(TermMapper::toDomain);
    }
}
