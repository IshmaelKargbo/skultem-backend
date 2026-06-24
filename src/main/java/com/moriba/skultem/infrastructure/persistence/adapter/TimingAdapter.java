package com.moriba.skultem.infrastructure.persistence.adapter;

import com.moriba.skultem.domain.model.Room;
import com.moriba.skultem.domain.model.Timing;
import com.moriba.skultem.domain.repository.RoomRepository;
import com.moriba.skultem.domain.repository.TimingRepository;
import com.moriba.skultem.infrastructure.persistence.entity.RoomEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.RoomJpaRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.TimingJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.RoomMapper;
import com.moriba.skultem.infrastructure.persistence.mapper.TimingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TimingAdapter implements TimingRepository {
    private final TimingJpaRepository repo;

    @Override
    public void save(Timing domain) {
        var entity = TimingMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Timing> findBySchoolId(String school) {
        return repo.findBySchoolId(school).map(TimingMapper::toDomain);
    }

    @Override
    public boolean existsBySchoolId(String schoolId) {
        return repo.existsBySchoolId(schoolId);
    }
}
