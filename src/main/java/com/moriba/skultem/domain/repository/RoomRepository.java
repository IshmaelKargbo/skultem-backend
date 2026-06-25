package com.moriba.skultem.domain.repository;

import com.moriba.skultem.domain.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface RoomRepository {
    void save(Room domain);

    Optional<Room> findByIdAndSchoolId(String id, String school);

    Page<Room> search(String school, String search, Pageable pageable);

    boolean existsByNameAndSchoolId(String name, String schoolId);

    void delete(Room domain);

    void softDelete(Room domain);
}
