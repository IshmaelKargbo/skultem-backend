package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.SaveReport;
import com.moriba.skultem.domain.repository.SaveReportRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.SaveReportJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.SaveReportMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SaveReportAdapter implements SaveReportRepository {
    private final SaveReportJpaRepository repo;

    @Override
    public void save(SaveReport domain) {
        repo.save(SaveReportMapper.toEntity(domain));
    }

    @Override
    public Optional<SaveReport> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findById(id)
                .filter(e -> schoolId.equals(e.getSchoolId()))
                .map(SaveReportMapper::toDomain);
    }

    @Override
    public Page<SaveReport> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(SaveReportMapper::toDomain);
    }

    @Override
    public void delete(SaveReport domain) {
        repo.deleteById(domain.getId());
    }

    @Override
    public boolean existsByName(String name, String schoolId) {
        return repo.existsByNameIgnoreCaseAndSchoolId(name, schoolId);
    }
}
