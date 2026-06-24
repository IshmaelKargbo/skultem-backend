package com.moriba.skultem.infrastructure.persistence.adapter;

import com.moriba.skultem.domain.model.Room;
import com.moriba.skultem.domain.model.Term;
import com.moriba.skultem.domain.model.Term.Status;
import com.moriba.skultem.domain.repository.RoomRepository;
import com.moriba.skultem.domain.repository.TermRepository;
import com.moriba.skultem.infrastructure.persistence.entity.RoomEntity;
import com.moriba.skultem.infrastructure.persistence.entity.TermEntity;
import com.moriba.skultem.infrastructure.persistence.jpa.RoomJpaRepository;
import com.moriba.skultem.infrastructure.persistence.jpa.TermJpaRepository;
import com.moriba.skultem.infrastructure.persistence.mapper.RoomMapper;
import com.moriba.skultem.infrastructure.persistence.mapper.TermMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
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

}
