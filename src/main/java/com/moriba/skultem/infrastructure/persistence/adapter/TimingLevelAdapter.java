package com.moriba.skultem.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.moriba.skultem.domain.model.TimingLevel;
import com.moriba.skultem.domain.repository.TimingLevelRepository;
import com.moriba.skultem.domain.vo.Level;
import com.moriba.skultem.infrastructure.persistence.jpa.TimingLevelJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.TimingLevelMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TimingLevelAdapter implements TimingLevelRepository {
    private final TimingLevelJpaRepository repo;

    @Override
    public void save(TimingLevel domain) {
        repo.save(TimingLevelMapper.toEntity(domain));
    }

    @Override
    public Optional<TimingLevel> findBySchoolIdAndLevel(String schoolId, Level level) {
        return repo.findBySchoolIdAndLevel(schoolId, level).map(TimingLevelMapper::toDomain);
    }

    @Override
    public List<TimingLevel> findAllBySchoolId(String schoolId) {
        return repo.findAllBySchoolId(schoolId).stream().map(TimingLevelMapper::toDomain).toList();
    }

    @Override
    public void deleteByTimingId(String timingId) {
        repo.deleteByTiming_Id(timingId);
    }

    @Override
    public void delete(TimingLevel domain) {
        repo.deleteById(domain.getId());
    }
}
