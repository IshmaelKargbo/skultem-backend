package com.moriba.skultem.infrastructure.persistence.adapter;

import com.moriba.skultem.domain.model.WorkingDay;
import com.moriba.skultem.domain.repository.WorkingDayRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.WorkingDayJpaRepository;
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
    public List<WorkingDay> findAllByTimingId(String timingId) {
        return repo.findAllByTiming_Id(timingId).stream().map(WorkingDayMapper::toDomain).toList();
    }

    @Override
    public Optional<WorkingDay> findByDayAndTimingId(WorkingDay.Day day, String timingId) {
        return repo.findByDayAndTiming_Id(day, timingId).map(WorkingDayMapper::toDomain);
    }

    @Override
    public boolean existsByDayAndTimingId(WorkingDay.Day day, String timingId) {
        return repo.existsByDayAndTiming_Id(day, timingId);
    }

    @Override
    public void deleteAllByTimingId(String timingId) {
        repo.deleteAllByTiming_Id(timingId);
    }
}
