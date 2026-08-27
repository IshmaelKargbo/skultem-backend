package com.moriba.skultem.infrastructure.persistence.adapter;

import com.moriba.skultem.domain.model.Week;
import com.moriba.skultem.domain.repository.WeekRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.WeekJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.WeekMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WeekAdapter implements WeekRepository {
    private final WeekJpaRepository repo;

    @Override
    public void save(Week domain) {
        var entity = WeekMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public List<Week> findAllByScheme(String scheme) {
        return repo.findBySchemeId(scheme).stream().map(WeekMapper::toDomain).toList();
    }

    @Override
    public List<Week> findAllBySchemeIds(List<String> schemeIds) {
        if (schemeIds.isEmpty())
            return List.of();

        return repo.findBySchemeIdIn(schemeIds).stream().map(WeekMapper::toDomain).toList();
    }

    @Override
    public Optional<Week> findById(String id) {
        return repo.findById(id).map(WeekMapper::toDomain);
    }

    @Override
    public boolean existsByWeekAndSchemeAndSchoolId(int week, String scheme, String school) {
        return repo.existsByWeekAndScheme_IdAndSchoolId(week, scheme, school);
    }

    @Override
    public Page<Week> findBySchemeSessionAcademicYear(String year, Pageable page) {
        return repo.findBySchemeSessionAcademicYearId(year, page).map(WeekMapper::toDomain);
    }
}
