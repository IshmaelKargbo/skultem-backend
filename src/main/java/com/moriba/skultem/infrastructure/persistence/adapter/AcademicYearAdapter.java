package com.moriba.skultem.infrastructure.persistence.adapter;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.AcademicYear;
import com.moriba.skultem.domain.repository.AcademicYearRepository;
import com.moriba.skultem.infrastructure.persistence.entity.AcademicYearEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.AcademicYearJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.AcademicYearMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AcademicYearAdapter implements AcademicYearRepository {

    private final AcademicYearJpaRepository repo;

    @Override
    public void save(AcademicYear domain) {
        AcademicYearEntity entity = AcademicYearMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<AcademicYear> findById(String id) {
        return repo.findById(id).map(AcademicYearMapper::toDomain);
    }

    @Override
    public boolean existsByNameAndSchool(String school, String name) {
        return repo.existsByNameIgnoreCaseAndSchoolId(school, name);
    }

    @Override
    public Page<AcademicYear> findAllBySchool(String school, Pageable pageable) {
        return repo.findAllBySchoolIdAndStatusNot(school, AcademicYear.Status.DELETED, pageable).map(AcademicYearMapper::toDomain);
    }

    @Override
    public void delete(AcademicYear domain) {
        AcademicYearEntity entity = AcademicYearMapper.toEntity(domain);
        repo.delete(entity);
    }

    @Override
    public void deactivateAllBySchool(String school) {
        repo.deactivateAllBySchoolId(school);
    }

    @Override
    public Optional<AcademicYear> findActiveBySchool(String school) {
        return repo.findBySchoolIdAndActiveTrue(school).map(AcademicYearMapper::toDomain);
    }

    @Override
    public Optional<AcademicYear> findByIdAndSchoolId(String id, String school) {
        return repo.findByIdAndSchoolId(id, school).map(AcademicYearMapper::toDomain);
    }

    @Override
    public Optional<AcademicYear> findNextBySchool(String school, LocalDate after) {
        return repo.findFirstBySchoolIdAndStartDateAfterAndStatusNotOrderByStartDateAsc(
                school, after, AcademicYear.Status.DELETED).map(AcademicYearMapper::toDomain);
    }

    @Override
    public Optional<AcademicYear> findPreviousBySchool(String school, LocalDate before) {
        return repo.findFirstBySchoolIdAndEndDateBeforeAndStatusNotOrderByEndDateDesc(
                school, before, AcademicYear.Status.DELETED).map(AcademicYearMapper::toDomain);
    }

    @Override
    public boolean existsAsNextYear(String school, String yearId) {
        return repo.existsByNextYear_IdAndSchoolId(yearId, school);
    }
}
