package com.moriba.skultem.infrastructure.persistence.adapter;

import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.model.WorkingDay;
import com.moriba.skultem.domain.repository.TimingRepository;
import com.moriba.skultem.domain.repository.WorkingDayRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.TimingJpaRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.WorkingDayJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.TimingMapper;
import com.moriba.skultem.infrastructure.persistence.mapper.WorkingDayMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WorkingDayAdapter implements WorkingDayRepository {
    private final WorkingDayJpaRepository repo;

    @Override
    public void save(WorkingDay domain) {
        var entity = WorkingDayMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public List<WorkingDay> findAllBySchoolId(String school) {
        return repo.findAllBySchoolId(school).stream().map(WorkingDayMapper::toDomain).toList();
    }

    @Override
    public Optional<WorkingDay> findByDayAndSchoolId(WorkingDay.Day day, String school) {
        return repo.findByDayAndSchoolId(day, school).map(WorkingDayMapper::toDomain);
    }

    @Override
    public boolean existsByDayAndSchoolId(WorkingDay.Day day, String schoolId) {
        return repo.existsByDayAndSchoolId(day, schoolId);
    }
}
