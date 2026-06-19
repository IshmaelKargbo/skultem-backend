package com.moriba.skultem.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.moriba.skultem.domain.model.House;

public interface HouseRepository {
    void save(House domain);

    void delete(House domain);

    boolean existByNameAndSchoolId(String name, String schoolId);

    Optional<House> findByIdAndSchool(String id, String schoolId);

    Page<House> findAllBySchoolId(String schoolId, Pageable pageable);
    
    Page<House> search(String value, String schoolId, Pageable pageable);

}
