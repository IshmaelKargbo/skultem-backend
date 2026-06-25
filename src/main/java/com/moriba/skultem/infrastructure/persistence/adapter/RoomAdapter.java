package com.moriba.skultem.infrastructure.persistence.adapter;

import com.moriba.skultem.domain.model.Room;
import com.moriba.skultem.domain.repository.RoomRepository;
import com.moriba.skultem.infrastructure.persistence.entity.RoomEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.RoomJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.RoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoomAdapter implements RoomRepository {

    private final RoomJpaRepository repo;

    @Override
    public void save(Room domain) {
        RoomEntity entity = RoomMapper.toEntity(domain);
        repo.save(entity);
    }

    @Override
    public Optional<Room> findByIdAndSchoolId(String id, String school) {
        return repo.findByIdAndSchoolId(id, school).map(RoomMapper::toDomain);
    }

    @Override
    public Page<Room> search(String school, String search, Pageable pageable) {
        return repo.search(school, search, pageable).map(RoomMapper::toDomain);
    }

    @Override
    public boolean existsByNameAndSchoolId(String name, String schoolId) {
        return repo.existsByNameIgnoreCaseAndSchoolId(name, schoolId);
    }

    @Override
    public void delete(Room domain) {
        var entity = RoomMapper.toEntity(domain);
        repo.delete(entity);
    }

    @Override
    public void softDelete(Room domain) {
        var entity = RoomMapper.toEntity(domain);
        repo.delete(entity);
    }

    
}
