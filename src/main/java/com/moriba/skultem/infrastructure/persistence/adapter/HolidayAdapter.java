package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.Holiday;
import com.moriba.skultem.domain.repository.HolidayRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.HolidayJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.HolidayMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class HolidayAdapter implements HolidayRepository {
    private final HolidayJpaRepository repo;

    @Override
    public void save(Holiday domain) {
        var entity = HolidayMapper.param(domain);
        repo.save(entity);
    }

    @Override
    public void delete(Holiday domain) {
        repo.deleteById(domain.getId());
    }

    @Override
    public boolean existByNameAndSchoolId(String name, String schoolId) {
        return repo.existsByNameIgnoreCaseAndSchoolId(name, schoolId);
    }

    @Override
    public Optional<Holiday> findByIdAndSchool(String id, String schoolId) {
        return repo.findByIdAndSchoolId(id, schoolId).map(HolidayMapper::toDomain);
    }

    @Override
    public Page<Holiday> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolIdOrderByCreatedAtDesc(schoolId, pageable).map(HolidayMapper::toDomain);
    }

    @Override
    public Page<Holiday> findAllBySchoolIdAndAcademicYear(String schoolId, String academicYearId, Pageable pageable) {
        return repo.findAllBySchoolIdAndAcademicYear_Id(schoolId, academicYearId, pageable).map(HolidayMapper::toDomain);
    }
}
