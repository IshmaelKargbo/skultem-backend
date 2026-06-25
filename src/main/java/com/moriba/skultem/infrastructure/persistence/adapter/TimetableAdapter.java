package com.moriba.skultem.infrastructure.persistence.adapter;

import com.moriba.skultem.domain.model.Timetable;
import com.moriba.skultem.domain.repository.TimetableRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.TimetableJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.TimetableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TimetableAdapter implements TimetableRepository {
    private final TimetableJpaRepository repo;

    @Override
    public void save(Timetable domain) {
        var entity = TimetableMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Timetable> findByPeriodIdAndDayIdAndSchoolId(String period, String day, String schoolId) {
        return repo.findByPeriodIdAndDayIdAndSchoolId(period, day, schoolId).map(TimetableMapper::toDomain);
    }

    @Override
    public List<Timetable> findAllByPeriodId(String period) {
        return repo.findByPeriodId(period).stream().map(TimetableMapper::toDomain).toList();
    }

    @Override
    public boolean existsByPeriodAndDayAndSchoolId(String period, String day, String schoolId) {
        return repo.existsByPeriodIdAndDayIdAndSchoolId(period, day, schoolId);
    }

    @Override
    public void delete(Timetable domain) {
        var entity = TimetableMapper.toEntity(domain);
        repo.delete(entity);
    }
}
