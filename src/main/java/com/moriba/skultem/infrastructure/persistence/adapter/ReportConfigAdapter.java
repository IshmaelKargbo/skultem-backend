package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.ReportConfig;
import com.moriba.skultem.domain.repository.ReportConfigRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.ReportConfigJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.ReportConfigMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReportConfigAdapter implements ReportConfigRepository {
    private final ReportConfigJpaRepository repo;

    @Override
    public void save(ReportConfig domain) {
        repo.save(ReportConfigMapper.toEntity(domain));
    }

    @Override
    public Optional<ReportConfig> findByIdAndSchoolId(String id, String schoolId) {
        return repo.findById(id)
                .filter(e -> schoolId.equals(e.getSchoolId()))
                .map(ReportConfigMapper::toDomain);
    }

    @Override
    public Page<ReportConfig> findAllBySchoolId(String schoolId, Pageable pageable) {
        return repo.findAllBySchoolId(schoolId, pageable).map(ReportConfigMapper::toDomain);
    }

    @Override
    public void delete(ReportConfig domain) {
        repo.deleteById(domain.getId());
    }
}
